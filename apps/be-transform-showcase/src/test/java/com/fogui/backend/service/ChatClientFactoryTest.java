package com.fogui.backend.service;

import com.fogui.starter.policy.FogUiGenerationPolicy;
import com.fogui.starter.policy.FogUiGenerationPolicyProperties;
import com.fogui.starter.policy.FogUiGenerationPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChatClientFactory.
 * Tests OpenAI-only client creation logic.
 */
@DisplayName("ChatClientFactory")
@ExtendWith(MockitoExtension.class)
class ChatClientFactoryTest {

    private ObjectProvider<List<Advisor>> advisorProvider(List<Advisor> advisors) {
        return new ObjectProvider<>() {
            @Override
            public @NonNull List<Advisor> getObject(@NonNull Object... args) {
                return Objects.requireNonNull(advisors);
            }

            @Override
            public @NonNull List<Advisor> getIfAvailable() {
                return Objects.requireNonNull(advisors);
            }

            @Override
            public @NonNull List<Advisor> getIfUnique() {
                return Objects.requireNonNull(advisors);
            }

            @Override
            public @NonNull List<Advisor> getObject() {
                return Objects.requireNonNull(advisors);
            }
        };
    }

    private FogUiGenerationPolicyService mockPolicyService() {
        return mock(FogUiGenerationPolicyService.class);
    }

    @Nested
    @DisplayName("getActiveModelName")
    class GetActiveModelName {

        @Test
        @DisplayName("should return configured OpenAI model")
        void shouldReturnConfiguredOpenAiModel() {
            OpenAiChatModel mockOpenAiModel = mock(OpenAiChatModel.class);
            ChatClientFactory factory = new ChatClientFactory(
                    mockOpenAiModel,
                    mockPolicyService(),
                    advisorProvider(List.of()));
            ReflectionTestUtils.setField(factory, "openAiModel", "gpt-4.1-nano");

            String modelName = factory.getActiveModelName();

            assertEquals("gpt-4.1-nano", modelName);
        }
    }

    @Nested
    @DisplayName("createClient")
    class CreateClientSuccess {

        @Test
        @DisplayName("should create ChatClient with OpenAI model")
        void shouldCreateChatClientWithOpenAiModel() {
            OpenAiChatModel mockOpenAiModel = mock(OpenAiChatModel.class);
            ChatClientFactory factory = new ChatClientFactory(
                    mockOpenAiModel,
                    mockPolicyService(),
                    advisorProvider(List.of()));
            ReflectionTestUtils.setField(factory, "openAiModel", "gpt-4.1-nano");

            ChatClient client = factory.createClient();

            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw when OpenAI model is not configured")
        void shouldThrowWhenOpenAiNotConfigured() {
            ChatClientFactory factory = new ChatClientFactory(
                    null,
                    mockPolicyService(),
                    advisorProvider(List.of()));

            assertThrows(IllegalStateException.class, factory::createClient);
        }

        @Test
        @DisplayName("should throw with helpful message")
        void shouldThrowWithAppropriateMessageForOpenAi() {
            ChatClientFactory factory = new ChatClientFactory(
                    null,
                    mockPolicyService(),
                    advisorProvider(List.of()));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    factory::createClient
            );

            assertTrue(exception.getMessage().contains("OpenAI"));
        }
    }

    @Test
    @DisplayName("buildDeterministicOptions should apply policy values")
    void buildDeterministicOptionsShouldApplyPolicyValues() {
        OpenAiChatModel mockOpenAiModel = mock(OpenAiChatModel.class);
        FogUiGenerationPolicyService service = mock(FogUiGenerationPolicyService.class);
        FogUiGenerationPolicy policy = new FogUiGenerationPolicy();
        policy.setModel("gpt-4.1-nano");
        policy.setTemperature(0.1);
        policy.setTopP(0.95);
        policy.setSeed(42);
        policy.setResponseFormat(FogUiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
        policy.setMaxTokens(1024);
        policy.setMaxCompletionTokens(512);
        when(service.resolve("gpt-4.1-nano")).thenReturn(policy);

        ChatClientFactory factory = new ChatClientFactory(
                mockOpenAiModel,
                service,
                advisorProvider(List.of()));
        ReflectionTestUtils.setField(factory, "openAiModel", "gpt-4.1-nano");

        var options = factory.buildDeterministicOptions();

        assertEquals("gpt-4.1-nano", options.getModel());
        assertEquals(0.1, options.getTemperature());
        assertEquals(0.95, options.getTopP());
        assertEquals(42, options.getSeed());
        assertNotNull(options.getResponseFormat());
        assertEquals(ResponseFormat.Type.JSON_OBJECT, options.getResponseFormat().getType());
        assertEquals(1024, options.getMaxTokens());
        assertEquals(512, options.getMaxCompletionTokens());
    }

    @Test
    @DisplayName("createClient should support configured advisor list")
    void createClientShouldSupportConfiguredAdvisors() {
        OpenAiChatModel mockOpenAiModel = mock(OpenAiChatModel.class);
        Advisor advisor = mock(Advisor.class);
        ChatClientFactory factory = new ChatClientFactory(
                mockOpenAiModel,
                mockPolicyService(),
                advisorProvider(List.of(advisor)));
        ReflectionTestUtils.setField(factory, "openAiModel", "gpt-4.1-nano");

        ChatClient client = factory.createClient();

        assertNotNull(client);
        @SuppressWarnings("unchecked")
        List<Advisor> configured = (List<Advisor>) ReflectionTestUtils.getField(factory, "defaultAdvisors");
        assertNotNull(configured);
        assertEquals(1, configured.size());
    }
}
