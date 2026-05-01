package com.fogui.starter.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fogui.starter.policy.FogUiChatOptionsPolicyApplier;
import com.fogui.starter.policy.FogUiGenerationPolicyProperties;
import com.fogui.starter.policy.FogUiGenerationPolicyService;
import com.fogui.starter.policy.FogUiProviderResolver;
import com.fogui.starter.policy.GenericChatOptionsCustomizer;
import com.fogui.starter.policy.OpenAiChatOptionsCustomizer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.lang.NonNull;
import org.springframework.mock.env.MockEnvironment;
import reactor.core.publisher.Flux;

@DisplayName("DeterministicOptionsAdvisor")
class DeterministicOptionsAdvisorTest {

  @Test
  void shouldApplyDeterministicOptionsForCallPath() {
    FogUiGenerationPolicyProperties properties = defaultProperties();
    properties.setSeed(7);
    properties.setMaxTokens(1200);
    properties.setMaxCompletionTokens(600);

    DeterministicOptionsAdvisor advisor = newAdvisor(properties);
    ChatClientRequest request = requestWithModel("gpt-4.1-nano");

    FixedCallChain chain = new FixedCallChain(emptyResponse());
    advisor.adviseCall(Objects.requireNonNull(request), chain);

    OpenAiChatOptions options =
        (OpenAiChatOptions)
            Objects.requireNonNull(
                Objects.requireNonNull(Objects.requireNonNull(chain.getCapturedRequest()).prompt())
                    .getOptions());
    assertThat(options.getModel()).isEqualTo("gpt-4.1-nano");
    assertThat(options.getTemperature()).isEqualTo(0.0);
    assertThat(options.getTopP()).isEqualTo(1.0);
    assertThat(options.getSeed()).isEqualTo(7);
    assertThat(options.getResponseFormat()).isNotNull();
    assertThat(options.getResponseFormat().getType()).isEqualTo(ResponseFormat.Type.JSON_OBJECT);
    assertThat(options.getMaxTokens()).isEqualTo(1200);
    assertThat(options.getMaxCompletionTokens()).isEqualTo(600);
  }

  @Test
  void shouldApplyDeterministicOptionsForStreamPath() {
    FogUiGenerationPolicyProperties properties = defaultProperties();
    DeterministicOptionsAdvisor advisor = newAdvisor(properties);
    ChatClientRequest request = requestWithModel("gpt-4.1-nano");

    FixedStreamChain chain = new FixedStreamChain(Flux.just(emptyResponse()));
    advisor.adviseStream(Objects.requireNonNull(request), chain).blockLast();

    OpenAiChatOptions options =
        (OpenAiChatOptions)
            Objects.requireNonNull(
                Objects.requireNonNull(Objects.requireNonNull(chain.getCapturedRequest()).prompt())
                    .getOptions());
    assertThat(options.getModel()).isEqualTo("gpt-4.1-nano");
    assertThat(options.getTemperature()).isEqualTo(0.0);
    assertThat(options.getTopP()).isEqualTo(1.0);
    assertThat(options.getResponseFormat()).isNotNull();
    assertThat(options.getResponseFormat().getType()).isEqualTo(ResponseFormat.Type.JSON_OBJECT);
  }

  @Test
  void shouldExposeStableOrderAndName() {
    DeterministicOptionsAdvisor advisor = newAdvisor(defaultProperties());
    assertThat(advisor.getOrder()).isEqualTo(FogUiAdvisorOrder.DETERMINISTIC_OPTIONS);
    assertThat(advisor.getName()).isEqualTo("foguiDeterministicOptionsAdvisor");
  }

  private DeterministicOptionsAdvisor newAdvisor(FogUiGenerationPolicyProperties properties) {
    FogUiChatOptionsPolicyApplier policyApplier =
        new FogUiChatOptionsPolicyApplier(
            new FogUiGenerationPolicyService(properties),
            new FogUiProviderResolver(new MockEnvironment()),
            List.of(new OpenAiChatOptionsCustomizer(), new GenericChatOptionsCustomizer()));
    return new DeterministicOptionsAdvisor(policyApplier);
  }

  private FogUiGenerationPolicyProperties defaultProperties() {
    FogUiGenerationPolicyProperties properties = new FogUiGenerationPolicyProperties();
    properties.setTemperature(0.0);
    properties.setTopP(1.0);
    return properties;
  }

  private ChatClientRequest requestWithModel(String model) {
    OpenAiChatOptions options = OpenAiChatOptions.builder().model(model).build();
    Prompt prompt = new Prompt("hello", options);
    return ChatClientRequest.builder()
        .prompt(prompt)
        .context(Objects.requireNonNull(Map.<String, Object>of()))
        .build();
  }

  private ChatClientResponse emptyResponse() {
    AssistantMessage assistantMessage = new AssistantMessage("{\"thinking\":[],\"content\":[]}");
    ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
    return ChatClientResponse.builder()
        .chatResponse(chatResponse)
        .context(Objects.requireNonNull(Map.<String, Object>of()))
        .build();
  }

  private static final class FixedCallChain implements CallAdvisorChain {
    private final ChatClientResponse response;
    private ChatClientRequest capturedRequest;

    private FixedCallChain(ChatClientResponse response) {
      this.response = response;
    }

    @Override
    public @NonNull ChatClientResponse nextCall(@NonNull ChatClientRequest chatClientRequest) {
      this.capturedRequest = chatClientRequest;
      return Objects.requireNonNull(response);
    }

    @Override
    public @NonNull List<CallAdvisor> getCallAdvisors() {
      return Objects.requireNonNull(List.of());
    }

    private ChatClientRequest getCapturedRequest() {
      return capturedRequest;
    }
  }

  private static final class FixedStreamChain implements StreamAdvisorChain {
    private final Flux<ChatClientResponse> responses;
    private ChatClientRequest capturedRequest;

    private FixedStreamChain(Flux<ChatClientResponse> responses) {
      this.responses = responses;
    }

    @Override
    public @NonNull Flux<ChatClientResponse> nextStream(
        @NonNull ChatClientRequest chatClientRequest) {
      this.capturedRequest = chatClientRequest;
      return Objects.requireNonNull(responses);
    }

    @Override
    public @NonNull List<StreamAdvisor> getStreamAdvisors() {
      return Objects.requireNonNull(List.of());
    }

    private ChatClientRequest getCapturedRequest() {
      return capturedRequest;
    }
  }
}
