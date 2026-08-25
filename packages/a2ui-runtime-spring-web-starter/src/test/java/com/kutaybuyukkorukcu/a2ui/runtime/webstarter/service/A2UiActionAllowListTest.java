package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiActionAllowListTest {

    @Test
    void fromHandlersUnionsNamesAndSkipsBlank() {
        A2UiActionHandler first = new NamedHandler(linkedNames("submit", " ", "", "approve"));
        A2UiActionHandler second = new NamedHandler(linkedNames("approve", "reject"));

        A2UiActionAllowList allowList = A2UiActionAllowList.fromHandlers(List.of(first, second));

        assertThat(allowList.isEmpty()).isFalse();
        assertThat(allowList.contains("submit")).isTrue();
        assertThat(allowList.contains("approve")).isTrue();
        assertThat(allowList.contains("reject")).isTrue();
        assertThat(allowList.contains(" ")).isFalse();
        assertThat(allowList.names()).containsExactly("submit", "approve", "reject");
        assertThatThrownBy(() -> allowList.names().add("invented"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyFromHandlersIsEmpty() {
        assertThat(A2UiActionAllowList.empty().isEmpty()).isTrue();
        assertThat(A2UiActionAllowList.fromHandlers(List.of()).isEmpty()).isTrue();
        assertThat(A2UiActionAllowList.fromHandlers(List.of(new NamedHandler(Set.of()))).isEmpty()).isTrue();
        assertThat(A2UiActionAllowList.empty().contains("submit")).isFalse();
        assertThat(A2UiActionAllowList.empty().names()).isEmpty();
    }

    @Test
    void extractActionNamesFromUpdateComponentsEventName() {
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root",
                                "Button",
                                Map.of("child", "label", "action", Map.of("event", Map.of("name", "submit")))))));

        assertThat(A2UiActionAllowList.extractActionNames(messages)).containsExactly("submit");
        assertThat(A2UiActionAllowList.fromHandlers(List.of(new NamedHandler(Set.of("save"))))
                .firstUnknownName(messages))
                .contains("submit");
        assertThat(A2UiActionAllowList.fromHandlers(List.of(new NamedHandler(Set.of("submit"))))
                .firstUnknownName(messages))
                .isEmpty();
        assertThat(A2UiActionAllowList.empty().firstUnknownName(messages)).isEmpty();
    }

    private static Set<String> linkedNames(String... names) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.addAll(List.of(names));
        return ordered;
    }

    private static final class NamedHandler implements A2UiActionHandler {
        private final Set<String> names;

        private NamedHandler(Set<String> names) {
            this.names = names;
        }

        @Override
        public Set<String> actionNames() {
            return names;
        }

        @Override
        public boolean supports(A2UiUserAction userAction) {
            return false;
        }

        @Override
        public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
            return List.of();
        }
    }
}
