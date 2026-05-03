package com.fogui.contract.a2ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class A2UiMessageValidatorTest {

    private final A2UiOutboundMapper mapper = new A2UiOutboundMapper();
    private final A2UiMessageValidator validator = new A2UiMessageValidator();

    @Test
    void shouldAcceptValidV08Messages() {
        GenerativeUIResponse response = GenerativeUIResponse.builder()
                .content(List.of(ContentBlock.text("hello")))
                .build();

        List<A2UiValidationError> diagnostics = validator.validate(
                mapper.toMessages(response),
                A2UiValidationContext.forVersion(A2UiProtocol.SUPPORTED_VERSION));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void shouldRejectMessagesWithMultipleEventPayloads() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(A2UiMessage.builder()
                        .surfaceUpdate(A2UiMessage.SurfaceUpdate.builder().surfaceId("main").build())
                        .beginRendering(A2UiMessage.BeginRendering.builder()
                                .surfaceId("main")
                                .root("root")
                                .catalogId(A2UiCatalogIds.CANONICAL_V0_8)
                                .build())
                        .build()));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.INVALID_MESSAGE_ENVELOPE.code(), diagnostics.getFirst().getCode());
    }

    @Test
    void shouldRejectUnsupportedRequestedVersion() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(),
                A2UiValidationContext.forVersion("0.9"));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.UNSUPPORTED_VERSION.code(), diagnostics.getFirst().getCode());
        assertEquals("0.9", diagnostics.getFirst().getDetails().get("requestedVersion"));
        assertEquals(A2UiProtocol.SUPPORTED_VERSION, diagnostics.getFirst().getDetails().get("supportedVersion"));
    }

    @Test
    void shouldRejectComponentDefinitionsWithoutIds() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(A2UiMessage.builder()
                        .surfaceUpdate(A2UiMessage.SurfaceUpdate.builder()
                                .surfaceId("main")
                                .components(List.of(A2UiMessage.ComponentDefinition.builder()
                                        .component(Map.of("Text", Map.of("text", Map.of("literalString", "hello"))))
                                        .build()))
                                .build())
                        .build()));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.MISSING_COMPONENT_ID.code(), diagnostics.getFirst().getCode());
    }

    @Test
    void shouldRejectUnknownComponentType() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(A2UiMessage.builder()
                        .surfaceUpdate(A2UiMessage.SurfaceUpdate.builder()
                                .surfaceId("main")
                                .components(List.of(A2UiMessage.ComponentDefinition.builder()
                                        .id("card-1")
                                        .component(Map.of("UnsupportedCard", Map.of("title", "Hello")))
                                        .build()))
                                .build())
                        .build()));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.UNKNOWN_COMPONENT_TYPE.code(), diagnostics.getFirst().getCode());
    }

    @Test
    void shouldRejectUnsupportedCatalogId() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(
                        A2UiMessage.builder()
                                .surfaceUpdate(A2UiMessage.SurfaceUpdate.builder()
                                        .surfaceId("main")
                                        .components(List.of(A2UiMessage.ComponentDefinition.builder()
                                                .id("root")
                                                .component(Map.of("Text", Map.of("text", Map.of("literalString", "hello"))))
                                                .build()))
                                        .build())
                                .build(),
                        A2UiMessage.builder()
                                .beginRendering(A2UiMessage.BeginRendering.builder()
                                        .surfaceId("main")
                                        .root("root")
                                        .catalogId("/a2ui/catalogs/unsupported/v0.8")
                                        .build())
                                .build()));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.UNSUPPORTED_CATALOG_ID.code(), diagnostics.getFirst().getCode());
    }

    @Test
    void shouldRejectBeginRenderingBeforeSurfaceUpdate() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(A2UiMessage.builder()
                        .beginRendering(A2UiMessage.BeginRendering.builder()
                                .surfaceId("main")
                                .root("root")
                                .catalogId(A2UiCatalogIds.CANONICAL_V0_8)
                                .build())
                        .build()));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.INVALID_MESSAGE_SEQUENCE.code(), diagnostics.getFirst().getCode());
    }

    @Test
    void shouldRejectBeginRenderingWithUnknownRoot() {
        List<A2UiValidationError> diagnostics = validator.validate(
                List.of(
                        A2UiMessage.builder()
                                .surfaceUpdate(A2UiMessage.SurfaceUpdate.builder()
                                        .surfaceId("main")
                                        .components(List.of(A2UiMessage.ComponentDefinition.builder()
                                                .id("header")
                                                .component(Map.of("Text", Map.of("text", Map.of("literalString", "hello"))))
                                                .build()))
                                        .build())
                                .build(),
                        A2UiMessage.builder()
                                .beginRendering(A2UiMessage.BeginRendering.builder()
                                        .surfaceId("main")
                                        .root("root")
                                        .catalogId(A2UiCatalogIds.CANONICAL_V0_8)
                                        .build())
                                .build()));

        assertEquals(1, diagnostics.size());
        assertEquals(A2UiValidationErrorCode.UNKNOWN_ROOT_COMPONENT.code(), diagnostics.getFirst().getCode());
    }
}