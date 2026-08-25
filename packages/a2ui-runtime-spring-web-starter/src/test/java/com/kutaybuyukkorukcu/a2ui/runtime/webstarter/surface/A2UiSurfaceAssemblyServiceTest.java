package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiFixedSurfaceSpec;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template.A2UiTemplateRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiSurfaceAssemblyServiceTest {

    private static final String TEMPLATE_ID = "confirm-button";

    private final A2UiMessageValidator validator = new A2UiMessageValidator();
    private final A2UiTemplateRegistry registry = A2UiTemplateRegistry.builder()
            .register(buttonTemplate())
            .build();

    @Test
    void shouldRejectUnknownActionWhenAllowListNonEmpty() {
        A2UiSurfaceAssemblyService restricted = new A2UiSurfaceAssemblyService(
                registry,
                validator,
                A2UiActionAllowList.fromHandlers(List.of(new NamedHandler(Set.of("save")))));

        assertThatThrownBy(() -> restricted.assemble(
                TEMPLATE_ID, "main", A2UiCatalogIds.BASIC_V0_9, Map.of()))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(SurfaceErrorCodes.UNKNOWN_ACTION);
    }

    @Test
    void shouldAssembleWhenAllowListContainsActionName() {
        A2UiSurfaceAssemblyService restricted = new A2UiSurfaceAssemblyService(
                registry,
                validator,
                A2UiActionAllowList.fromHandlers(List.of(new NamedHandler(Set.of("submit")))));

        List<A2UiMessage> messages = restricted.assemble(
                TEMPLATE_ID, "main", A2UiCatalogIds.BASIC_V0_9, Map.of());

        assertThat(messages).isNotEmpty();
        assertThat(A2UiActionAllowList.extractActionNames(messages)).containsExactly("submit");
    }

    private static A2UiTemplateDefinition buttonTemplate() {
        return new A2UiTemplateDefinition(
                TEMPLATE_ID,
                "Confirm button",
                Set.of(),
                Set.of(),
                () -> A2UiFixedSurfaceSpec.builder(TEMPLATE_ID, "root")
                        .components(() -> List.of(
                                new ComponentDefinition(
                                        "root",
                                        "Column",
                                        Map.of("children", List.of("go"), "justify", "start")),
                                new ComponentDefinition(
                                        "go",
                                        "Button",
                                        Map.of(
                                                "child", "go-label",
                                                "action", Map.of("event", Map.of("name", "submit")),
                                                "variant", "primary")),
                                new ComponentDefinition(
                                        "go-label",
                                        "Text",
                                        Map.of("text", "Go"))))
                        .build());
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
