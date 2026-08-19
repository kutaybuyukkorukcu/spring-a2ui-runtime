package com.kutaybuyukkorukcu.a2ui.runtime.validation;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage.ComponentDefinition;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiProtocol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiMessageValidatorTest {

    private final A2UiMessageValidator validator = new A2UiMessageValidator();

    @Test
    void shouldValidateCreateThenUpdateWithRoot() {
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new ComponentDefinition("root", "Text", Map.of("text", "Hello")))));
        assertThat(validator.validate(messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9)))
                .isEmpty();
    }

    @Test
    void shouldRejectUpdateWithoutCreateSurface() {
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.UpdateComponents("main", List.of(
                        new ComponentDefinition("root", "Text", Map.of("text", "Hello")))));
        List<A2UiDiagnostic> diagnostics = validator.validate(messages);
        assertThat(diagnostics).anyMatch(d -> "INVALID_MESSAGE_SEQUENCE".equals(d.code()));
    }

    @Test
    void shouldRejectMissingRootComponent() {
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new ComponentDefinition("title", "Text", Map.of("text", "Hello")))));
        List<A2UiDiagnostic> diagnostics = validator.validate(messages);
        assertThat(diagnostics).anyMatch(d -> "UNKNOWN_ROOT_COMPONENT".equals(d.code()));
    }

    @Test
    void shouldRejectBlankSurfaceIdOnCreate() {
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(
                new A2UiMessage.CreateSurface("", A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "MISSING_SURFACE_ID".equals(d.code()));
    }

    @Test
    void shouldRejectMissingCatalogIdOnCreate() {
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(
                new A2UiMessage.CreateSurface("main", null));
        assertThat(diagnostics).anyMatch(d -> "MISSING_CATALOG_ID".equals(d.code()));
    }

    @Test
    void shouldRejectUnsupportedCatalogId() {
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(
                new A2UiMessage.CreateSurface("main", "https://unknown.catalog/v1"));
        assertThat(diagnostics).anyMatch(d -> "UNSUPPORTED_CATALOG_ID".equals(d.code()));
    }

    @Test
    void shouldValidateUpdateDataModel() {
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new ComponentDefinition("root", "Text", Map.of("text", "Hi")))),
                new A2UiMessage.UpdateDataModel("main", "/", Map.of("name", "Alice")));
        assertThat(validator.validate(messages)).isEmpty();
    }

    @Test
    void shouldRejectBlankPathInUpdateDataModel() {
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(
                new A2UiMessage.UpdateDataModel("main", "", Map.of("x", 1)));
        assertThat(diagnostics).anyMatch(d -> "INVALID_DATA_UPDATE".equals(d.code()));
    }

    @Test
    void shouldAllowNullPathInUpdateDataModel() {
        assertThat(validator.validateSingle(
                new A2UiMessage.UpdateDataModel("main", null, Map.of("x", 1)))).isEmpty();
    }

    @Test
    void shouldRejectUnknownComponentType() {
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new ComponentDefinition("root", "NotARealType", Map.of()))));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "UNKNOWN_COMPONENT_TYPE".equals(d.code()));
    }

    @Test
    void shouldTreatEmptyCatalogSchemaAsUnknownComponentType() {
        Map<String, Map<String, Object>> ghost = Map.of("Ghost", Map.of());
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.of(Map.of("host-catalog", ghost));
        A2UiMessageValidator emptySchemaValidator = new A2UiMessageValidator(registry);
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", "host-catalog"),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new ComponentDefinition("root", "Ghost", Map.of()))));
        List<A2UiDiagnostic> diagnostics = emptySchemaValidator.validate(
                messages, A2UiValidationContext.forCatalog("host-catalog"));
        assertThat(diagnostics).anyMatch(d -> "UNKNOWN_COMPONENT_TYPE".equals(d.code()));
    }

    @Test
    void shouldRejectUnsupportedVersion() {
        List<A2UiDiagnostic> diagnostics = validator.validate(
                List.of(new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9)),
                A2UiValidationContext.forVersion("0.8"));
        assertThat(diagnostics).anyMatch(d -> "UNSUPPORTED_VERSION".equals(d.code()));
    }

    @Test
    void shouldAcceptSupportedVersions() {
        assertThat(validator.validate(
                List.of(
                        new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                        new A2UiMessage.UpdateComponents("main", List.of(
                                new ComponentDefinition("root", "Text", Map.of("text", "Hi"))))),
                A2UiValidationContext.forVersion(A2UiProtocol.SUPPORTED_VERSION))).isEmpty();
    }

    @Test
    void shouldRejectCheckBoxMissingRequiredLabel() {
        ComponentDefinition checkbox = new ComponentDefinition(
                "root", "CheckBox", Map.of("value", Map.of("path", "/enabled")));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(checkbox)));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "MISSING_REQUIRED_PROP".equals(d.code()));
    }

    @Test
    void shouldAcceptCheckBoxWithNativeLiteralAndPath() {
        ComponentDefinition checkbox = new ComponentDefinition(
                "root",
                "CheckBox",
                Map.of("label", "Notify me", "value", Map.of("path", "/notify")));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(checkbox)));
        assertThat(validator.validate(messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9)))
                .isEmpty();
    }

    @Test
    void shouldRejectTextFieldMissingRequiredValue() {
        ComponentDefinition textField = new ComponentDefinition(
                "root", "TextField", Map.of("label", "Summary"));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(textField)));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "MISSING_REQUIRED_PROP".equals(d.code()));
    }

    @Test
    void shouldAcceptTextFieldWithValuePath() {
        ComponentDefinition textField = new ComponentDefinition(
                "root",
                "TextField",
                Map.of("label", "Summary", "value", Map.of("path", "/summary")));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(textField)));
        assertThat(validator.validate(messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9)))
                .isEmpty();
    }

    @Test
    void shouldRejectTextWithUnknownProp() {
        ComponentDefinition text = new ComponentDefinition(
                "root", "Text", Map.of("text", "Hi", "notARealProp", true));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(text)));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "UNKNOWN_PROP".equals(d.code()));
    }

    @Test
    void shouldRejectInvalidTextVariantEnum() {
        ComponentDefinition text = new ComponentDefinition(
                "root", "Text", Map.of("text", "Hi", "variant", "huge"));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(text)));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "INVALID_ENUM_VALUE".equals(d.code()));
    }

    @Test
    void shouldRejectButtonMissingAction() {
        ComponentDefinition button = new ComponentDefinition(
                "root", "Button", Map.of("child", "label"));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(button)));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "MISSING_REQUIRED_PROP".equals(d.code()));
    }

    @Test
    void shouldAcceptButtonWithEventAction() {
        ComponentDefinition button = new ComponentDefinition(
                "root",
                "Button",
                Map.of(
                        "child", "label",
                        "action", Map.of("event", Map.of("name", "submit")),
                        "variant", "primary"));
        ComponentDefinition label = new ComponentDefinition("label", "Text", Map.of("text", "Go"));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(button, label)));
        assertThat(validator.validate(messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9)))
                .isEmpty();
    }

    @Test
    void shouldAcceptColumnWithBareChildrenArray() {
        ComponentDefinition column = new ComponentDefinition(
                "root", "Column", Map.of("children", List.of("a"), "justify", "start"));
        ComponentDefinition child = new ComponentDefinition("a", "Text", Map.of("text", "A"));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(column, child)));
        assertThat(validator.validate(messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9)))
                .isEmpty();
    }

    @Test
    void shouldRejectColumnWithExplicitListShape() {
        ComponentDefinition column = new ComponentDefinition(
                "root", "Column", Map.of("children", Map.of("explicitList", List.of("a"))));
        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(column)));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).isNotEmpty();
    }

    @Test
    void shouldRejectDeleteSurfaceWithoutSurfaceId() {
        List<A2UiDiagnostic> diagnostics = validator.validateSingle(new A2UiMessage.DeleteSurface(""));
        assertThat(diagnostics).anyMatch(d -> "MISSING_SURFACE_ID".equals(d.code()));
    }
}
