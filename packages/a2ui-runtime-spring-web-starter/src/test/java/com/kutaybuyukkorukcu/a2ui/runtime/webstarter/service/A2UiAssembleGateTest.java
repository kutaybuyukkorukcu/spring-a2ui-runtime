package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiAssembleGateTest {

    private static final String HOST = "https://example.com/catalogs/host/1.0";

    private final A2UiCatalogRegistry catalogs = A2UiCatalogRegistry.withContributions(
            A2UiCatalogRegistry.shared(), List.of(hostCatalog()));
    private final A2UiActionAllowList submitOnly = A2UiActionAllowList.fromHandlers(List.of(new A2UiActionHandler() {
        @Override
        public java.util.Set<com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName> eventNames() {
            return java.util.Set.of(new com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName("submit"));
        }

        @Override
        public boolean supports(com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction action) {
            return false;
        }

        @Override
        public List<A2UiMessage> handle(com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction action, String requestId) {
            return List.of();
        }
    }));

    @Test
    void unknownEventAndUnknownFunctionUseDifferentCodes() {
        assertThatThrownBy(() -> A2UiAssembleGate.check(List.of(button(Map.of(
                "child", "label",
                "action", Map.of("event", Map.of("name", "nope"))))), submitOnly, catalogs, HOST))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(A2UiErrorCode.UNKNOWN_ACTION.code());

        assertThatThrownBy(() -> A2UiAssembleGate.check(List.of(button(Map.of(
                "child", "label",
                "action", Map.of("functionCall", Map.of("@call", "missingFn"))))), submitOnly, catalogs, HOST))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(A2UiErrorCode.UNKNOWN_FUNCTION.code());
    }

    @Test
    void agentOnlyOnAButtonIsInvalidAndOpenUrlIsNotAnEvent() {
        assertThatThrownBy(() -> A2UiAssembleGate.check(List.of(button(Map.of(
                "child", "label",
                "action", Map.of("functionCall", Map.of("@call", "lockAccount"))))), submitOnly, catalogs, HOST))
                .isInstanceOf(SurfaceExecutionException.class)
                .extracting(ex -> ((SurfaceExecutionException) ex).getErrorCode())
                .isEqualTo(A2UiErrorCode.INVALID_FUNCTION_CALL.code());

        assertThatCode(() -> A2UiAssembleGate.check(List.of(button(Map.of(
                "child", "label",
                "action", Map.of("functionCall", Map.of("@call", "openUrl"))))), submitOnly, catalogs, A2UiCatalogIds.BASIC_V1))
                .doesNotThrowAnyException();
    }

    @Test
    void emptyEventAllowListIsFailOpenForEvents() {
        assertThatCode(() -> A2UiAssembleGate.check(
                List.of(button(Map.of(
                        "child", "label",
                        "action", Map.of("event", Map.of("name", "anything"))))),
                A2UiActionAllowList.empty(),
                catalogs,
                A2UiCatalogIds.BASIC_V1))
                .doesNotThrowAnyException();
    }

    private static A2UiMessage.UpdateComponents button(Map<String, Object> properties) {
        return new A2UiMessage.UpdateComponents("main", List.of(
                new A2UiMessage.ComponentDefinition("root", "Button", properties)));
    }

    private static A2UiCatalogContribution hostCatalog() {
        return new A2UiCatalogContribution() {
            @Override
            public String catalogId() {
                return HOST;
            }

            @Override
            public Map<String, Map<String, Object>> componentSchemas() {
                return Map.of("Note", Map.of("type", "object"));
            }

            @Override
            public Map<String, Map<String, Object>> functions() {
                return Map.of(
                        "lockAccount", Map.of("allowedCallers", "agentOnly"),
                        "quote", Map.of("allowedCallers", "rendererOrAgent"));
            }
        };
    }
}
