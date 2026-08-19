package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiGenerationPolicyDynamicModeTest {

    @Test
    void shouldUseNoneResponseFormatWhenGenerationModeIsDynamic() {
        A2UiGenerationPolicyProperties properties = new A2UiGenerationPolicyProperties();
        properties.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
        properties.setGenerationMode("dynamic");

        A2UiGenerationPolicy policy = new A2UiGenerationPolicyService(properties).resolve("gpt-4o");

        assertThat(policy.getResponseFormat()).isEqualTo(A2UiGenerationPolicyProperties.ResponseFormatMode.NONE);
    }

    @Test
    void shouldKeepConfiguredResponseFormatWhenGenerationModeIsTemplate() {
        A2UiGenerationPolicyProperties properties = new A2UiGenerationPolicyProperties();
        properties.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
        properties.setGenerationMode("template");

        A2UiGenerationPolicy policy = new A2UiGenerationPolicyService(properties).resolve("gpt-4o");

        assertThat(policy.getResponseFormat()).isEqualTo(A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
    }

    @Test
    void shouldPreferWebRuntimeGenerationModeOverStarterProperty() {
        A2UiGenerationPolicyProperties properties = new A2UiGenerationPolicyProperties();
        properties.setResponseFormat(A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
        properties.setGenerationMode("dynamic");
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("a2ui.web.runtime.generation-mode", "template");

        A2UiGenerationPolicy policy = new A2UiGenerationPolicyService(properties, environment).resolve("gpt-4o");

        assertThat(policy.getResponseFormat()).isEqualTo(A2UiGenerationPolicyProperties.ResponseFormatMode.JSON_OBJECT);
    }
}
