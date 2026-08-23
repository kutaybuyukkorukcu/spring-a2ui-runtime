package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiApplicationPolicyTest {

    @Test
    void noneRequiresNoConfirmationAndHidesNothing() {
        A2UiActionPolicy actionPolicy = A2UiActionPolicy.none();
        A2UiSurfacePolicy surfacePolicy = A2UiSurfacePolicy.none();
        List<A2UiMessage> messages = updateComponents("AccountBalance");

        assertThat(actionPolicy.requiresConfirmation("approve")).isFalse();
        assertThat(actionPolicy.requiresConfirmation("submit")).isFalse();
        assertThat(surfacePolicy.hiddenComponentTypes()).isEmpty();
        assertThat(A2UiComponentVisibility.firstHiddenType(messages, surfacePolicy)).isEmpty();
    }

    @Test
    void customActionPolicyRequiresConfirmationForApprove() {
        A2UiActionPolicy actionPolicy = name -> "approve".equals(name);

        assertThat(actionPolicy.requiresConfirmation("approve")).isTrue();
        assertThat(actionPolicy.requiresConfirmation("submit")).isFalse();
    }

    @Test
    void customSurfacePolicyHidesAccountBalanceInUpdateComponents() {
        A2UiSurfacePolicy surfacePolicy = () -> Set.of("AccountBalance");
        List<A2UiMessage> messages = updateComponents("AccountBalance");

        assertThat(surfacePolicy.hiddenComponentTypes()).containsExactly("AccountBalance");
        assertThat(A2UiComponentVisibility.firstHiddenType(messages, surfacePolicy))
                .contains("AccountBalance");
        assertThat(A2UiComponentVisibility.firstHiddenType(updateComponents("Text"), surfacePolicy))
                .isEmpty();
    }

    private static List<A2UiMessage> updateComponents(String componentType) {
        return List.of(new A2UiMessage.UpdateComponents(
                "main",
                List.of(new A2UiMessage.ComponentDefinition("root", componentType, Map.of("text", "x")))));
    }
}
