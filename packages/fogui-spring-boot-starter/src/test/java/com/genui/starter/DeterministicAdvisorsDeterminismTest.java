package com.genui.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.genui.contract.CanonicalValidationError;
import com.genui.model.genui.GenerativeUIResponse;
import com.genui.starter.advisor.CanonicalValidationAdvisor;
import com.genui.starter.advisor.DeterministicOptionsAdvisor;
import com.genui.starter.advisor.FogUiAdvisorContextKeys;
import com.genui.starter.advisor.FogUiAdvisorErrorCodes;
import com.genui.starter.advisor.FogUiAdvisorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Deterministic Advisors")
class DeterministicAdvisorsDeterminismTest {

    private static final int REPETITIONS = 8;
    private static final String MODEL_NAME = "gpt-4.1-nano";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FogUiCoreAutoConfiguration.class));

    private final ObjectMapper sortedMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @Test
    void shouldProduceDeterministicNormalizedResponsesThroughAdvisorChain() {
        contextRunner
                .withPropertyValues(
                        "fogui.deterministic.temperature=0.0",
                        "fogui.deterministic.top-p=1.0",
                        "fogui.deterministic.seed=17",
                        "fogui.advisors.fail-fast=true")
                .run(context -> {
                    DeterministicOptionsAdvisor optionsAdvisor = context.getBean(DeterministicOptionsAdvisor.class);
                    CanonicalValidationAdvisor validationAdvisor = context.getBean(CanonicalValidationAdvisor.class);

                    CapturingChatModel model = new CapturingChatModel(
                            "{\"thinking\":[],\"content\":[{\"type\":\"text\",\"value\":\"Stable\"}]}"
                    );

                    ChatClient chatClient = ChatClient.builder(model)
                            .defaultAdvisors(optionsAdvisor, validationAdvisor)
                            .build();

                    String baselineFingerprint = null;
                    for (int i = 0; i < REPETITIONS; i++) {
                        String rawCanonicalJson = chatClient
                                .prompt(promptWithNonDeterministicOptions())
                                .advisors(spec -> spec
                                        .param(FogUiAdvisorContextKeys.REQUEST_ID, "req-advisor-1")
                                        .param(FogUiAdvisorContextKeys.ROUTE_MODE, FogUiAdvisorContextKeys.ROUTE_TRANSFORM))
                                .call()
                                .content();

                        String fingerprint = canonicalFingerprint(rawCanonicalJson);
                        if (i == 0) {
                            baselineFingerprint = fingerprint;
                        } else {
                            assertEquals(baselineFingerprint, fingerprint);
                        }
                    }

                    assertNotNull(baselineFingerprint);
                    assertTrue(baselineFingerprint.contains("\"contractVersion\":\"fogui/1.0\""));
                    assertEquals(REPETITIONS, model.observedOptions.size());

                    for (OpenAiChatOptions options : model.observedOptions) {
                        assertEquals(MODEL_NAME, options.getModel());
                        assertEquals(0.0, options.getTemperature());
                        assertEquals(1.0, options.getTopP());
                        assertEquals(17, options.getSeed());
                    }
                });
    }

    @Test
    void shouldEmitDeterministicValidationFailureAcrossRepeatedCalls() {
        contextRunner
                .withPropertyValues(
                        "fogui.advisors.fail-fast=true",
                        "fogui.deterministic.temperature=0.0",
                        "fogui.deterministic.top-p=1.0")
                .run(context -> {
                    DeterministicOptionsAdvisor optionsAdvisor = context.getBean(DeterministicOptionsAdvisor.class);
                    CanonicalValidationAdvisor validationAdvisor = context.getBean(CanonicalValidationAdvisor.class);

                    CapturingChatModel model = new CapturingChatModel(
                            "{\"thinking\":[],\"content\":[{\"type\":\"unknown\"}]}"
                    );

                    ChatClient chatClient = ChatClient.builder(model)
                            .defaultAdvisors(optionsAdvisor, validationAdvisor)
                            .build();

                    String baselineDiagnostics = null;
                    for (int i = 0; i < REPETITIONS; i++) {
                        try {
                            chatClient
                                    .prompt(promptWithNonDeterministicOptions())
                                    .advisors(spec -> spec
                                            .param(FogUiAdvisorContextKeys.REQUEST_ID, "req-advisor-fail")
                                            .param(FogUiAdvisorContextKeys.ROUTE_MODE, FogUiAdvisorContextKeys.ROUTE_TRANSFORM))
                                    .call()
                                    .content();
                            fail("Expected deterministic advisor validation failure");
                        } catch (Exception ex) {
                            FogUiAdvisorException advisorException = findAdvisorException(ex);
                            assertNotNull(advisorException);
                            assertEquals(FogUiAdvisorErrorCodes.CANONICAL_VALIDATION_FAILED, advisorException.getErrorCode());

                            Map<String, Object> details = castDetails(advisorException.getDetails());
                            assertEquals("req-advisor-fail", details.get("requestId"));
                            assertEquals(FogUiAdvisorContextKeys.ROUTE_TRANSFORM, details.get("routeMode"));

                            @SuppressWarnings("unchecked")
                            List<CanonicalValidationError> diagnostics =
                                    (List<CanonicalValidationError>) details.get("diagnostics");
                            assertNotNull(diagnostics);
                            assertFalse(diagnostics.isEmpty());

                            String diagnosticFingerprint = diagnostics.stream()
                                    .map(diagnostic -> diagnostic.getPath()
                                            + "|"
                                            + diagnostic.getCode()
                                            + "|"
                                            + diagnostic.getCategory())
                                    .collect(Collectors.joining("||"));

                            if (i == 0) {
                                baselineDiagnostics = diagnosticFingerprint;
                            } else {
                                assertEquals(baselineDiagnostics, diagnosticFingerprint);
                            }
                        }
                    }

                    assertNotNull(baselineDiagnostics);
                });
    }

    private Prompt promptWithNonDeterministicOptions() {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(MODEL_NAME)
                .build();
        options.setTemperature(0.77);
        options.setTopP(0.31);
        options.setSeed(999);
        return new Prompt("Transform this content", options);
    }

    private String canonicalFingerprint(String rawCanonicalJson) {
        try {
            GenerativeUIResponse response = sortedMapper.readValue(rawCanonicalJson, GenerativeUIResponse.class);
            return sortedMapper.writeValueAsString(response);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fingerprint canonical response", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castDetails(Object details) {
        return (Map<String, Object>) details;
    }

    private FogUiAdvisorException findAdvisorException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof FogUiAdvisorException advisorException) {
                return advisorException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static final class CapturingChatModel implements ChatModel {

        private final String assistantJson;
        private final List<OpenAiChatOptions> observedOptions = new ArrayList<>();

        private CapturingChatModel(String assistantJson) {
            this.assistantJson = assistantJson;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatOptions options = prompt.getOptions();
            if (options instanceof OpenAiChatOptions openAiChatOptions) {
                observedOptions.add(OpenAiChatOptions.fromOptions(openAiChatOptions));
            } else {
                observedOptions.add(OpenAiChatOptions.builder().build());
            }

            AssistantMessage assistantMessage = new AssistantMessage(assistantJson);
            return new ChatResponse(List.of(new Generation(assistantMessage)));
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return OpenAiChatOptions.builder().model(MODEL_NAME).build();
        }
    }
}