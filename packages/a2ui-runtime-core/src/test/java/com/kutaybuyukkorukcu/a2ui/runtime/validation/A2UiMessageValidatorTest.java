package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.DataEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiMessageValidatorTest {

    private final A2UiMessageValidator validator = new A2UiMessageValidator();

    private static Map<String, Object> validTextComponent() {
        return Map.of("Text", Map.of("text", Map.of("literalString", "Hello")));
    }

    @Test
    void shouldValidateValidSurfaceUpdate() {
        ComponentDefinition text = new ComponentDefinition("txt-1", Map.of("Text", Map.of("text", Map.of("literalString", "Hello"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldRejectNullMessageBatch() {
        List<A2UiDiagnostic> diagnostics = validator.validate(null);
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.get(0).code()).isEqualTo(A2UiErrorCode.NULL_MESSAGE_BATCH.code());
    }

    @Test
    void shouldRejectEmptySurfaceUpdate() {
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of());
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldRejectMissingSurfaceId() {
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("", List.of());
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.MISSING_SURFACE_ID.code()));
    }

    @Test
    void shouldRejectUnknownComponentType() {
        ComponentDefinition cd = new ComponentDefinition("c1", Map.of("Container", Map.of()));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(cd));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.UNKNOWN_COMPONENT_TYPE.code()));
    }

    @Test
    void shouldValidateBeginRenderingSequence() {
        ComponentDefinition text = new ComponentDefinition("root-1", Map.of("Text", Map.of("text", Map.of("literalString", "Hi"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "root-1", A2UiCatalogIds.STANDARD_V0_8, null);

        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su, br));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldRejectBeginRenderingWithoutPrecedingSurfaceUpdate() {
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "root-1", A2UiCatalogIds.STANDARD_V0_8, null);
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(br));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.INVALID_MESSAGE_SEQUENCE.code()));
    }

    @Test
    void shouldRejectBeginRenderingWithUnknownRoot() {
        ComponentDefinition text = new ComponentDefinition("txt-1", Map.of("Text", Map.of("text", Map.of("literalString", "Hi"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "unknown-root", A2UiCatalogIds.STANDARD_V0_8, null);

        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su, br));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.UNKNOWN_ROOT_COMPONENT.code()));
    }

    @Test
    void shouldValidateDataModelUpdate() {
        DataEntry entry = DataEntry.ofString("name", "Alice");
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", "user", List.of(entry));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(dmu));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldRejectDataModelUpdateWithBlankPath() {
        DataEntry entry = DataEntry.ofString("name", "Alice");
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", "", List.of(entry));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(dmu));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.INVALID_DATA_UPDATE.code()));
    }

    @Test
    void shouldValidateDeleteSurface() {
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("main");
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(ds));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldRejectDeleteSurfaceWithMissingId() {
        A2UiMessage.DeleteSurface ds = new A2UiMessage.DeleteSurface("");
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(ds));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.MISSING_SURFACE_ID.code()));
    }

    @Test
    void shouldRejectUnsupportedVersion() {
        A2UiValidationContext ctx = A2UiValidationContext.forVersion("0.7");
        ComponentDefinition text = new ComponentDefinition("t1", validTextComponent());
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su), ctx);
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.UNSUPPORTED_VERSION.code()));
    }

    @Test
    void shouldAcceptSupportedVersion() {
        A2UiValidationContext ctx = A2UiValidationContext.forVersion("0.8");
        ComponentDefinition text = new ComponentDefinition("t1", validTextComponent());
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su), ctx);
        assertThat(diagnostics).noneMatch(d -> d.code().equals(A2UiErrorCode.UNSUPPORTED_VERSION.code()));
    }

    @Test
    void isValidShouldReturnTrueForValidMessages() {
        ComponentDefinition text = new ComponentDefinition("t1", validTextComponent());
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        assertThat(validator.isValid(List.of(su))).isTrue();
    }

    @Test
    void isValidShouldReturnFalseForInvalidMessages() {
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("", List.of());
        assertThat(validator.isValid(List.of(su))).isFalse();
    }

    @Test
    void shouldValidateFullSurfaceLifecycle() {
        ComponentDefinition text = new ComponentDefinition("root-1", Map.of("Text", Map.of("text", Map.of("literalString", "Hello"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        DataEntry entry = DataEntry.ofString("greeting", "Hello");
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null, List.of(entry));
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "root-1", A2UiCatalogIds.STANDARD_V0_8, null);

        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su, dmu, br));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedCatalogId() {
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "root-1", "https://unknown.catalog/v1", null);
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(br));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.UNSUPPORTED_CATALOG_ID.code()));
    }

    @Test
    void shouldRejectMissingComponentId() {
        ComponentDefinition cd = new ComponentDefinition("", validTextComponent());
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(cd));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.MISSING_COMPONENT_ID.code()));
    }

    @Test
    void shouldAllowNullPathInDataModelUpdate() {
        DataEntry entry = DataEntry.ofString("key", "value");
        A2UiMessage.DataModelUpdate dmu = new A2UiMessage.DataModelUpdate("main", null, List.of(entry));
        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(dmu));
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldValidateSingleMessageValidSurfaceUpdate() {
        ComponentDefinition text = new ComponentDefinition("txt-1", Map.of("Text", Map.of("text", Map.of("literalString", "Hello"))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(su);
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void shouldValidateSingleMessageMissingSurfaceId() {
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate(null, List.of());
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(su);
        assertThat(diagnostics).anyMatch(d -> d.code().equals(A2UiErrorCode.MISSING_SURFACE_ID.code()));
    }

    @Test
    void shouldValidateSingleMessageBeginRenderingWithValidCatalog() {
        A2UiMessage.BeginRendering br = new A2UiMessage.BeginRendering("main", "root", A2UiCatalogIds.STANDARD_V0_8, null);
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(br);
        assertThat(diagnostics).noneMatch(d -> d.code().equals(A2UiErrorCode.UNSUPPORTED_CATALOG_ID.code()));
    }

    @Test
    void shouldRejectInvalidComponentEnumValue() {
        ComponentDefinition text = new ComponentDefinition(
                "txt-1",
                Map.of("Text", Map.of(
                        "text", Map.of("literalString", "Hello"),
                        "usageHint", "headline")));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(text));

        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).anyMatch(d -> d.path().contains("usageHint")
                && d.code().equals(A2UiErrorCode.INVALID_COMPONENT_PAYLOAD.code()));
    }

    @Test
    void shouldRejectChildrenWithBothExplicitListAndTemplate() {
        ComponentDefinition row = new ComponentDefinition(
                "row-1",
                Map.of("Row", Map.of(
                        "children", Map.of(
                                "explicitList", List.of("a"),
                                "template", Map.of("dataBinding", "/items", "componentId", "item-template")))));
        A2UiMessage.SurfaceUpdate su = new A2UiMessage.SurfaceUpdate("main", List.of(row));

        List<A2UiDiagnostic> diagnostics = validator.validate(List.of(su));
        assertThat(diagnostics).anyMatch(d -> d.path().contains("children")
                && d.code().equals(A2UiErrorCode.INVALID_COMPONENT_PAYLOAD.code()));
    }

}