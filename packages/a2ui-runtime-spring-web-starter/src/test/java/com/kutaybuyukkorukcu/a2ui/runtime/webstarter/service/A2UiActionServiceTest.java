package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.*;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class A2UiActionServiceTest {

    private A2UiActionHandler handler;
    private A2UiRuntimeMetrics metrics;
    private A2UiMessageValidator validator;
    private A2UiActionService service;

    @BeforeEach
    void setUp() {
        handler = mock(A2UiActionHandler.class);
        metrics = mock(A2UiRuntimeMetrics.class);
        validator = mock(A2UiMessageValidator.class);
        service = new A2UiActionService(List.of(handler), metrics, validator);
    }

    @Test
    void shouldHandleUserAction() {
        A2UiUserAction userAction = new A2UiUserAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);
        List<A2UiMessage> responseMessages = List.of(new A2UiMessage.CreateSurface("main", "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"));

        when(handler.supports(userAction)).thenReturn(true);
        when(handler.handle(any(), anyString())).thenReturn(responseMessages);
        when(validator.validate(any(), any(A2UiValidationContext.class))).thenReturn(List.of());

        A2UiActionResponse result = service.handleClientEvent(event, "req-1");

        assertThat(result.accepted()).isTrue();
        assertThat(result.actionName()).isEqualTo("submit");
        assertThat(result.surfaceId()).isEqualTo("main");
        assertThat(result.messageCount()).isEqualTo(1);
        verify(validator).validate(
                responseMessages,
                A2UiValidationContext.forVersionAndCatalog(A2UiProtocol.SUPPORTED_VERSION, A2UiCatalogIds.BASIC_V0_9));
    }

    @Test
    void shouldValidateAcksAgainstCatalogFromCreateSurface() {
        A2UiUserAction userAction = new A2UiUserAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);
        String hostCatalog = "https://example.com/catalogs/host/1.0";
        List<A2UiMessage> responseMessages = List.of(new A2UiMessage.CreateSurface("main", hostCatalog));

        when(handler.supports(userAction)).thenReturn(true);
        when(handler.handle(any(), anyString())).thenReturn(responseMessages);
        when(validator.validate(any(), any(A2UiValidationContext.class))).thenReturn(List.of());

        service.handleClientEvent(event, "req-1");

        verify(validator).validate(
                responseMessages,
                A2UiValidationContext.forVersionAndCatalog(A2UiProtocol.SUPPORTED_VERSION, hostCatalog));
    }

    @Test
    void shouldThrowOnNullEvent() {
        assertThatThrownBy(() -> service.handleClientEvent(null, "req-1"))
                .isInstanceOf(A2UiActionException.class);
    }

    @Test
    void shouldThrowOnMissingUserAction() {
        A2UiClientEvent event = new A2UiClientEvent(null, new A2UiClientError("ERR", "main", null, "test error", null));
        A2UiActionResponse result = service.handleClientEvent(event, "req-1");
        assertThat(result.accepted()).isTrue();
        assertThat(result.errorCode()).isEqualTo("ERR");
    }

    @Test
    void shouldThrowWhenNoHandlerFound() {
        A2UiUserAction userAction = new A2UiUserAction("unknown", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);

        when(handler.supports(userAction)).thenReturn(false);

        assertThatThrownBy(() -> service.handleClientEvent(event, "req-1"))
                .isInstanceOf(A2UiActionException.class);
    }

    @Test
    void shouldThrowWhenHandlerReturnsInvalidMessages() {
        A2UiUserAction userAction = new A2UiUserAction("click", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);
        List<A2UiMessage> messages = List.of(new A2UiMessage.CreateSurface("main", "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"));

        when(handler.supports(userAction)).thenReturn(true);
        when(handler.handle(any(), anyString())).thenReturn(messages);
        when(validator.validate(any(), any(A2UiValidationContext.class))).thenReturn(List.of(mock(A2UiDiagnostic.class)));

        assertThatThrownBy(() -> service.handleClientEvent(event, "req-1"))
                .isInstanceOf(A2UiActionException.class);
    }

    @Test
    void shouldRejectHostTypeUnderBasicCatalogWithRealValidator() {
        String hostCatalog = "https://example.com/catalogs/host/1.0";
        A2UiMessageValidator realValidator = new A2UiMessageValidator(registryWithStatusBadge(hostCatalog));
        A2UiActionService realService = new A2UiActionService(List.of(handler), metrics, realValidator);
        A2UiUserAction userAction = new A2UiUserAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root", "StatusBadge", Map.of("text", "Approved")))));

        when(handler.supports(userAction)).thenReturn(true);
        when(handler.handle(any(), anyString())).thenReturn(messages);

        assertThatThrownBy(() -> realService.handleClientEvent(event, "req-1"))
                .isInstanceOf(A2UiActionException.class)
                .extracting(ex -> ((A2UiActionException) ex).getErrorCode())
                .isEqualTo(A2UiActionErrorCodes.INVALID_ACTION_RESPONSE);
    }

    @Test
    void shouldAcceptHostTypeWhenCreateSurfacePinsHostCatalog() {
        String hostCatalog = "https://example.com/catalogs/host/1.0";
        A2UiMessageValidator realValidator = new A2UiMessageValidator(registryWithStatusBadge(hostCatalog));
        A2UiActionService realService = new A2UiActionService(List.of(handler), metrics, realValidator);
        A2UiUserAction userAction = new A2UiUserAction("submit", "main", "btn-1", null, Map.of());
        A2UiClientEvent event = new A2UiClientEvent(userAction, null);
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", hostCatalog),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root", "StatusBadge", Map.of("text", "Approved")))));

        when(handler.supports(userAction)).thenReturn(true);
        when(handler.handle(any(), anyString())).thenReturn(messages);

        A2UiActionResponse result = realService.handleClientEvent(event, "req-1");
        assertThat(result.accepted()).isTrue();
        assertThat(result.messageCount()).isEqualTo(2);
    }

    private static A2UiCatalogRegistry registryWithStatusBadge(String hostCatalog) {
        return A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(),
                List.of(new A2UiCatalogContribution() {
                    @Override
                    public String catalogId() {
                        return hostCatalog;
                    }

                    @Override
                    public Map<String, Map<String, Object>> componentSchemas() {
                        return Map.of("StatusBadge", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("text"),
                                "properties", Map.of("text", Map.of("type", "string"))));
                    }
                }));
    }
}