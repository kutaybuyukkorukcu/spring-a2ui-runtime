package com.fogui.starter.advisor;

import com.fogui.starter.policy.FogUiChatOptionsPolicyApplier;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * Applies deterministic generation policy to both call and stream advisor paths.
 */
public class DeterministicOptionsAdvisor implements BaseAdvisor {

    private final FogUiChatOptionsPolicyApplier chatOptionsPolicyApplier;

    public DeterministicOptionsAdvisor(FogUiChatOptionsPolicyApplier chatOptionsPolicyApplier) {
        this.chatOptionsPolicyApplier = chatOptionsPolicyApplier;
    }

    @Override
    public @NonNull ChatClientRequest before(@NonNull ChatClientRequest request, @NonNull AdvisorChain advisorChain) {
        Prompt prompt = Objects.requireNonNull(request.prompt());

        ChatOptions deterministicOptions = chatOptionsPolicyApplier.apply(prompt.getOptions());
        Prompt updatedPrompt = Objects.requireNonNull(prompt.mutate().chatOptions(deterministicOptions).build());
        return request.mutate().prompt(updatedPrompt).build();
    }

    @Override
    public @NonNull ChatClientResponse after(
            @NonNull ChatClientResponse response,
            @NonNull AdvisorChain advisorChain
    ) {
        return response;
    }

    @Override
    public int getOrder() {
        return FogUiAdvisorOrder.DETERMINISTIC_OPTIONS;
    }

    @Override
    public @NonNull String getName() {
        return "foguiDeterministicOptionsAdvisor";
    }
}

