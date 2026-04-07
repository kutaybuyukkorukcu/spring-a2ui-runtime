package com.genui.starter.advisor;

import com.genui.starter.policy.FogUiGenerationPolicyProperties;
import com.genui.starter.policy.FogUiGenerationPolicyService;
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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeterministicOptionsAdvisor")
class DeterministicOptionsAdvisorTest {

    @Test
    void shouldApplyDeterministicOptionsForCallPath() {
        FogUiGenerationPolicyProperties properties = defaultProperties();
        properties.setSeed(7);
        properties.setMaxTokens(1200);
        properties.setMaxCompletionTokens(600);

        DeterministicOptionsAdvisor advisor = new DeterministicOptionsAdvisor(new FogUiGenerationPolicyService(properties));
        ChatClientRequest request = requestWithModel("gpt-4.1-nano");

        FixedCallChain chain = new FixedCallChain(emptyResponse());
        advisor.adviseCall(request, chain);

        OpenAiChatOptions options = (OpenAiChatOptions) chain.getCapturedRequest().prompt().getOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4.1-nano");
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getTopP()).isEqualTo(1.0);
        assertThat(options.getSeed()).isEqualTo(7);
        assertThat(options.getMaxTokens()).isEqualTo(1200);
        assertThat(options.getMaxCompletionTokens()).isEqualTo(600);
    }

    @Test
    void shouldApplyDeterministicOptionsForStreamPath() {
        FogUiGenerationPolicyProperties properties = defaultProperties();
        DeterministicOptionsAdvisor advisor = new DeterministicOptionsAdvisor(new FogUiGenerationPolicyService(properties));
        ChatClientRequest request = requestWithModel("gpt-4.1-nano");

        FixedStreamChain chain = new FixedStreamChain(Flux.just(emptyResponse()));
        advisor.adviseStream(request, chain).blockLast();

        OpenAiChatOptions options = (OpenAiChatOptions) chain.getCapturedRequest().prompt().getOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4.1-nano");
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getTopP()).isEqualTo(1.0);
    }

    @Test
    void shouldExposeStableOrderAndName() {
        DeterministicOptionsAdvisor advisor = new DeterministicOptionsAdvisor(
                new FogUiGenerationPolicyService(defaultProperties()));
        assertThat(advisor.getOrder()).isEqualTo(FogUiAdvisorOrder.DETERMINISTIC_OPTIONS);
        assertThat(advisor.getName()).isEqualTo("foguiDeterministicOptionsAdvisor");
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
                .context(Map.of())
                .build();
    }

    private ChatClientResponse emptyResponse() {
        AssistantMessage assistantMessage = new AssistantMessage("{\"thinking\":[],\"content\":[]}");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(Map.of())
                .build();
    }

    private static final class FixedCallChain implements CallAdvisorChain {
        private final ChatClientResponse response;
        private ChatClientRequest capturedRequest;

        private FixedCallChain(ChatClientResponse response) {
            this.response = response;
        }

        @Override
        public ChatClientResponse nextCall(ChatClientRequest chatClientRequest) {
            this.capturedRequest = chatClientRequest;
            return response;
        }

        @Override
        public List<CallAdvisor> getCallAdvisors() {
            return List.of();
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
        public Flux<ChatClientResponse> nextStream(ChatClientRequest chatClientRequest) {
            this.capturedRequest = chatClientRequest;
            return responses;
        }

        @Override
        public List<StreamAdvisor> getStreamAdvisors() {
            return List.of();
        }

        private ChatClientRequest getCapturedRequest() {
            return capturedRequest;
        }
    }
}

