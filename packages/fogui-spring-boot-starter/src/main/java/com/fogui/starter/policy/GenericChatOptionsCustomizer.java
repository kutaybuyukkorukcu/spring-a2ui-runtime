package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

public class GenericChatOptionsCustomizer implements FogUiChatOptionsCustomizer {

    @Override
    public boolean supports(@NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions) {
        return true;
    }

    @Override
    public @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy) {
        if (incomingOptions == null || isDefaultChatOptions(incomingOptions)) {
            return buildDefaultChatOptions(incomingOptions, policy);
        }

        ChatOptions copiedOptions = Objects.requireNonNull(incomingOptions.copy());
        applyPolicy(copiedOptions, policy);
        return copiedOptions;
    }

    private @NonNull ChatOptions buildDefaultChatOptions(
            @Nullable ChatOptions incomingOptions,
            @NonNull FogUiGenerationPolicy policy
    ) {
        ChatOptions.Builder builder = ChatOptions.builder();
        copyGenericOptions(builder, incomingOptions);
        applyPolicy(builder, policy);
        return Objects.requireNonNull(builder.build());
    }

    private void copyGenericOptions(ChatOptions.Builder builder, @Nullable ChatOptions incomingOptions) {
        if (incomingOptions == null) {
            return;
        }

        applyIfPresent(incomingOptions.getModel(), builder::model);
        applyIfPresent(incomingOptions.getTemperature(), builder::temperature);
        applyIfPresent(incomingOptions.getTopP(), builder::topP);
        applyIfPresent(incomingOptions.getMaxTokens(), builder::maxTokens);
    }

    private void applyPolicy(ChatOptions.Builder builder, @NonNull FogUiGenerationPolicy policy) {
        applyTextIfPresent(policy.getModel(), builder::model);
        applyIfPresent(policy.getTemperature(), builder::temperature);
        applyIfPresent(policy.getTopP(), builder::topP);
        applyIfPresent(policy.getMaxTokens(), builder::maxTokens);
    }

    private void applyPolicy(ChatOptions copiedOptions, @NonNull FogUiGenerationPolicy policy) {
        applyTextIfPresent(policy.getModel(), value -> setIfPresent(copiedOptions, "setModel", String.class, value));
        applyIfPresent(policy.getTemperature(), value -> setIfPresent(copiedOptions, "setTemperature", Double.class, value));
        applyIfPresent(policy.getTopP(), value -> setIfPresent(copiedOptions, "setTopP", Double.class, value));
        applyIfPresent(policy.getMaxTokens(), value -> setIfPresent(copiedOptions, "setMaxTokens", Integer.class, value));
    }

    private void applyTextIfPresent(@Nullable String value, Consumer<String> applier) {
        if (StringUtils.hasText(value)) {
            applier.accept(Objects.requireNonNull(value));
        }
    }

    private <T> void applyIfPresent(@Nullable T value, Consumer<T> applier) {
        if (value != null) {
            applier.accept(value);
        }
    }

    private boolean isDefaultChatOptions(@NonNull ChatOptions incomingOptions) {
        return incomingOptions.getClass().getSimpleName().contains("DefaultChatOptions");
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private <T> void setIfPresent(Object target, String methodName, Class<T> parameterType, @Nullable T value) {
        if (value == null) {
            return;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        } catch (NoSuchMethodException ignored) {
            // Provider-specific options may not expose every common setter.
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Failed to apply deterministic option " + methodName + " on " + target.getClass().getName(),
                    ex);
        }
    }
}