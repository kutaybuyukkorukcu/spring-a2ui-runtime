package com.fogui.starter.policy;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;

public class GenericChatOptionsCustomizer implements FogUiChatOptionsCustomizer {

    @Override
    public boolean supports(@NonNull FogUiProviderType providerType, @Nullable ChatOptions incomingOptions) {
        return true;
    }

    @Override
    public @NonNull ChatOptions customize(@Nullable ChatOptions incomingOptions, @NonNull FogUiGenerationPolicy policy) {
        if (incomingOptions == null) {
            ChatOptions.Builder builder = ChatOptions.builder();
            if (StringUtils.hasText(policy.getModel())) {
                builder.model(Objects.requireNonNull(policy.getModel()));
            }
            if (policy.getTemperature() != null) {
                builder.temperature(policy.getTemperature());
            }
            if (policy.getTopP() != null) {
                builder.topP(policy.getTopP());
            }
            if (policy.getMaxTokens() != null) {
                builder.maxTokens(policy.getMaxTokens());
            }
            return Objects.requireNonNull(builder.build());
        }

        ChatOptions copiedOptions = Objects.requireNonNull(incomingOptions.copy());
        setIfPresent(copiedOptions, "setModel", String.class, policy.getModel());
        setIfPresent(copiedOptions, "setTemperature", Double.class, policy.getTemperature());
        setIfPresent(copiedOptions, "setTopP", Double.class, policy.getTopP());
        setIfPresent(copiedOptions, "setMaxTokens", Integer.class, policy.getMaxTokens());
        return copiedOptions;
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