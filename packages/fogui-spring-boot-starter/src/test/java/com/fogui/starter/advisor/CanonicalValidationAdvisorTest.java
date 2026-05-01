package com.fogui.starter.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.CanonicalValidationError;
import com.fogui.contract.FogUiCanonicalValidator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@DisplayName("CanonicalValidationAdvisor")
class CanonicalValidationAdvisorTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldStampContractVersionForValidCanonicalResponse() {
    CanonicalValidationAdvisor advisor =
        new CanonicalValidationAdvisor(new FogUiCanonicalValidator(), objectMapper, true);

    FixedCallChain chain = new FixedCallChain(responseWithJson("{\"thinking\":[],\"content\":[]}"));
    ChatClientRequest request =
        requestWithContext("req-1", FogUiAdvisorContextKeys.ROUTE_TRANSFORM);

    ChatClientResponse response = assertDoesNotThrow(() -> advisor.adviseCall(request, chain));
    String outputText =
        Objects.requireNonNull(response.chatResponse()).getResult().getOutput().getText();

    assertThat(outputText).contains("\"contractVersion\":\"fogui/1.0\"");
  }

  @Test
  void shouldFailFastWithStableDiagnosticsOnValidationFailure() {
    CanonicalValidationAdvisor advisor =
        new CanonicalValidationAdvisor(new FogUiCanonicalValidator(), objectMapper, true);

    FixedCallChain chain =
        new FixedCallChain(
            responseWithJson("{\"thinking\":[],\"content\":[{\"type\":\"unknown\"}]}"));
    ChatClientRequest request =
        requestWithContext("req-validation", FogUiAdvisorContextKeys.ROUTE_TRANSFORM);

    FogUiAdvisorException exception =
        assertThrows(FogUiAdvisorException.class, () -> advisor.adviseCall(request, chain));

    assertThat(exception.getErrorCode())
        .isEqualTo(FogUiAdvisorErrorCodes.CANONICAL_VALIDATION_FAILED);
    assertThat(exception.getDetails()).isInstanceOf(Map.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) exception.getDetails();
    assertThat(details)
        .containsEntry("requestId", "req-validation")
        .containsEntry("routeMode", FogUiAdvisorContextKeys.ROUTE_TRANSFORM)
        .containsEntry("expectedContractVersion", "fogui/1.0");

    @SuppressWarnings("unchecked")
    List<CanonicalValidationError> diagnostics =
        (List<CanonicalValidationError>) details.get("diagnostics");
    assertThat(diagnostics).isNotEmpty();
    assertThat(diagnostics.getFirst().getCode()).isEqualTo("UNSUPPORTED_TYPE");
  }

  @Test
  void shouldRejectLegacyComponentShorthandEvenWhenComponentTypeIsPresent() {
    CanonicalValidationAdvisor advisor =
        new CanonicalValidationAdvisor(new FogUiCanonicalValidator(), objectMapper, true);

    FixedCallChain chain =
        new FixedCallChain(
            responseWithJson(
                "{"
                    + "\"thinking\":[],"
                    + "\"content\":[{"
                    + "\"type\":\"card\","
                    + "\"componentType\":\"Card\","
                    + "\"props\":{}"
                    + "}]}"));
    ChatClientRequest request =
        requestWithContext("req-legacy-type", FogUiAdvisorContextKeys.ROUTE_TRANSFORM);

    FogUiAdvisorException exception =
        assertThrows(FogUiAdvisorException.class, () -> advisor.adviseCall(request, chain));

    assertThat(exception.getErrorCode())
        .isEqualTo(FogUiAdvisorErrorCodes.CANONICAL_VALIDATION_FAILED);

    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) exception.getDetails();
    @SuppressWarnings("unchecked")
    List<CanonicalValidationError> diagnostics =
        (List<CanonicalValidationError>) details.get("diagnostics");
    assertThat(diagnostics).isNotEmpty();
    assertThat(diagnostics.getFirst().getPath()).isEqualTo("$.content[0].type");
    assertThat(diagnostics.getFirst().getCode()).isEqualTo("UNSUPPORTED_TYPE");
  }

  @Test
  void shouldRejectExplicitContractVersionMismatchBeforeStamping() {
    CanonicalValidationAdvisor advisor =
        new CanonicalValidationAdvisor(new FogUiCanonicalValidator(), objectMapper, true);

    FixedCallChain chain =
        new FixedCallChain(
            responseWithJson(
                "{"
                    + "\"thinking\":[],"
                    + "\"content\":[{\"type\":\"text\",\"value\":\"Stable\"}],"
                    + "\"metadata\":{\"contractVersion\":\"fogui/0.9\"}"
                    + "}"));
    ChatClientRequest request =
        requestWithContext("req-version-mismatch", FogUiAdvisorContextKeys.ROUTE_TRANSFORM);

    FogUiAdvisorException exception =
        assertThrows(FogUiAdvisorException.class, () -> advisor.adviseCall(request, chain));

    assertThat(exception.getErrorCode())
        .isEqualTo(FogUiAdvisorErrorCodes.CANONICAL_VALIDATION_FAILED);

    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) exception.getDetails();
    @SuppressWarnings("unchecked")
    List<CanonicalValidationError> diagnostics =
        (List<CanonicalValidationError>) details.get("diagnostics");
    assertThat(diagnostics).isNotEmpty();
    assertThat(diagnostics.getFirst().getCode()).isEqualTo("CONTRACT_VERSION_MISMATCH");
  }

  @Test
  void shouldFailFastOnMalformedJson() {
    CanonicalValidationAdvisor advisor =
        new CanonicalValidationAdvisor(new FogUiCanonicalValidator(), objectMapper, true);

    FixedCallChain chain = new FixedCallChain(responseWithJson("{not-json"));
    ChatClientRequest request =
        requestWithContext("req-parse", FogUiAdvisorContextKeys.ROUTE_TRANSFORM);

    FogUiAdvisorException exception =
        assertThrows(FogUiAdvisorException.class, () -> advisor.adviseCall(request, chain));

    assertThat(exception.getErrorCode()).isEqualTo(FogUiAdvisorErrorCodes.CANONICAL_PARSE_FAILED);
  }

  @Test
  void shouldNotThrowWhenFailFastDisabled() {
    CanonicalValidationAdvisor advisor =
        new CanonicalValidationAdvisor(new FogUiCanonicalValidator(), objectMapper, false);

    FixedCallChain chain =
        new FixedCallChain(
            responseWithJson("{\"thinking\":[],\"content\":[{\"type\":\"unknown\"}]}"));
    ChatClientRequest request =
        requestWithContext("req-soft", FogUiAdvisorContextKeys.ROUTE_TRANSFORM);

    assertDoesNotThrow(() -> advisor.adviseCall(request, chain));
  }

  private ChatClientRequest requestWithContext(String requestId, String route) {
    return ChatClientRequest.builder()
        .prompt(new Prompt("transform"))
        .context(
            Map.<String, Object>of(
                FogUiAdvisorContextKeys.REQUEST_ID, requestId,
                FogUiAdvisorContextKeys.ROUTE_MODE, route))
        .build();
  }

  private ChatClientResponse responseWithJson(String json) {
    AssistantMessage message = new AssistantMessage(Objects.requireNonNull(json));
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(message)));
    return ChatClientResponse.builder()
        .chatResponse(chatResponse)
        .context(Collections.emptyMap())
        .build();
  }

  private static final class FixedCallChain implements CallAdvisorChain {
    private final ChatClientResponse response;

    private FixedCallChain(ChatClientResponse response) {
      this.response = response;
    }

    @Override
    public ChatClientResponse nextCall(ChatClientRequest chatClientRequest) {
      return response;
    }

    @Override
    public List<CallAdvisor> getCallAdvisors() {
      return List.of();
    }
  }
}
