package com.fogui.starter.advisor;

import com.fogui.starter.policy.FogUiGenerationPolicy;
import com.fogui.starter.policy.FogUiGenerationPolicyService;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Applies deterministic generation policy to both call and stream advisor paths.
 */
public class DeterministicOptionsAdvisor implements BaseAdvisor {

    private final FogUiGenerationPolicyService generationPolicyService;

    public DeterministicOptionsAdvisor(FogUiGenerationPolicyService generationPolicyService) {
        this.generationPolicyService = generationPolicyService;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Prompt prompt = request.prompt();
        if (prompt == null) {
            return request;
        }

        OpenAiChatOptions deterministicOptions = buildDeterministicOptions(prompt.getOptions());
        Prompt updatedPrompt = prompt.mutate().chatOptions(deterministicOptions).build();
        return request.mutate().prompt(updatedPrompt).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    private OpenAiChatOptions buildDeterministicOptions(ChatOptions incomingOptions) {
        String requestedModel = readModel(incomingOptions);
        FogUiGenerationPolicy policy = generationPolicyService.resolve(requestedModel);

        OpenAiChatOptions options = incomingOptions instanceof OpenAiChatOptions openAiOptions
                ? OpenAiChatOptions.fromOptions(openAiOptions)
                : OpenAiChatOptions.builder().build();

        if (requestedModel != null && !requestedModel.isBlank()) {
            options.setModel(requestedModel);
        } else if (policy.getModel() != null && !policy.getModel().isBlank()) {
            options.setModel(policy.getModel());
        }

        options.setTemperature(policy.getTemperature());
        options.setTopP(policy.getTopP());
        options.setSeed(policy.getSeed());
        options.setMaxTokens(policy.getMaxTokens());
        options.setMaxCompletionTokens(policy.getMaxCompletionTokens());
        return options;
    }

    private String readModel(ChatOptions options) {
        if (options instanceof OpenAiChatOptions openAiOptions) {
            return openAiOptions.getModel();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return FogUiAdvisorOrder.DETERMINISTIC_OPTIONS;
    }

    @Override
    public String getName() {
        return "foguiDeterministicOptionsAdvisor";
    }
}

