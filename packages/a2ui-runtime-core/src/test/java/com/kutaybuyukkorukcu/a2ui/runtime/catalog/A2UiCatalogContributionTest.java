package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiDiagnostic;
import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiValidationContext;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.validation.A2UiMessageValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiCatalogContributionTest {

    private static final String HOST_CATALOG_ID = "https://example.com/catalogs/hitl/1.0";

    private static A2UiCatalogContribution statusBadgeContribution() {
        return new A2UiCatalogContribution() {
            @Override
            public String catalogId() {
                return HOST_CATALOG_ID;
            }

            @Override
            public Map<String, Map<String, Object>> componentSchemas() {
                Map<String, Object> statusBadgeSchema = Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("text"),
                        "properties", Map.of(
                                "text", Map.of("type", "string"),
                                "tone", Map.of("type", "string")));
                return Map.of("StatusBadge", statusBadgeSchema);
            }

            @Override
            public String rulesText() {
                return "StatusBadge renders a small pill; keep text under 24 characters.";
            }
        };
    }

    @Test
    void shouldRegisterHostCatalogAndComponentType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));

        assertThat(registry.isSupportedCatalogId(HOST_CATALOG_ID)).isTrue();
        assertThat(registry.supportsComponentType("StatusBadge")).isTrue();
        assertThat(registry.componentTypesForCatalog(HOST_CATALOG_ID)).contains("StatusBadge");
    }

    @Test
    void shouldKeepBasicCatalogIntactAlongsideContribution() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));

        assertThat(registry.isSupportedCatalogId(A2UiCatalogIds.BASIC_V0_9)).isTrue();
        assertThat(registry.supportsComponentType("Text")).isTrue();
        assertThat(registry.supportsComponentType("StatusBadge")).isTrue();
    }

    @Test
    void shouldAppendContributionRulesTextAfterBasicRules() {
        String basicRulesText = A2UiCatalogRegistry.shared().catalogRulesText();
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));

        String rulesText = registry.catalogRulesText();
        assertThat(rulesText).startsWith(basicRulesText);
        assertThat(rulesText).endsWith("StatusBadge renders a small pill; keep text under 24 characters.");
    }

    @Test
    void shouldValidateRegisteredHostComponentType() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));
        A2UiMessageValidator validator = new A2UiMessageValidator(registry);
        A2UiValidationContext context = A2UiValidationContext.forCatalog(HOST_CATALOG_ID);

        List<A2UiMessage> validMessages = List.of(
                new A2UiMessage.CreateSurface("main", HOST_CATALOG_ID),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root", "StatusBadge", Map.of("text", "Approved")))));
        assertThat(validator.validate(validMessages, context)).isEmpty();
    }

    @Test
    void shouldRejectHostComponentTypeUnderBasicCatalog() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));
        A2UiMessageValidator validator = new A2UiMessageValidator(registry);

        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root", "StatusBadge", Map.of("text", "Approved")))));
        List<A2UiDiagnostic> diagnostics = validator.validate(
                messages, A2UiValidationContext.forCatalog(A2UiCatalogIds.BASIC_V0_9));
        assertThat(diagnostics).anyMatch(d -> "UNKNOWN_COMPONENT_TYPE".equals(d.code()));
    }

    @Test
    void shouldRejectHostComponentTypeWhenCatalogDerivedFromCreateSurface() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));
        A2UiMessageValidator validator = new A2UiMessageValidator(registry);

        List<A2UiMessage> messages = List.of(
                new A2UiMessage.CreateSurface("main", A2UiCatalogIds.BASIC_V0_9),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root", "StatusBadge", Map.of("text", "Approved")))));
        List<A2UiDiagnostic> diagnostics = validator.validate(messages);
        assertThat(diagnostics).anyMatch(d -> "UNKNOWN_COMPONENT_TYPE".equals(d.code()));
    }

    @Test
    void shouldFailValidationWhenRequiredPropMissing() {
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(statusBadgeContribution()));
        A2UiMessageValidator validator = new A2UiMessageValidator(registry);
        A2UiValidationContext context = A2UiValidationContext.forCatalog(HOST_CATALOG_ID);

        List<A2UiMessage> invalidMessages = List.of(
                new A2UiMessage.CreateSurface("main", HOST_CATALOG_ID),
                new A2UiMessage.UpdateComponents("main", List.of(
                        new A2UiMessage.ComponentDefinition(
                                "root", "StatusBadge", Map.of("tone", "success")))));
        List<A2UiDiagnostic> diagnostics = validator.validate(invalidMessages, context);
        assertThat(diagnostics).isNotEmpty();
    }

    @Test
    void shouldIgnoreBlankOrNullContributions() {
        A2UiCatalogContribution blankId = new A2UiCatalogContribution() {
            @Override
            public String catalogId() {
                return " ";
            }

            @Override
            public Map<String, Map<String, Object>> componentSchemas() {
                return Map.of("Ignored", Map.of());
            }
        };

        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), java.util.Arrays.asList(blankId, null));

        assertThat(registry.supportsComponentType("Ignored")).isFalse();
        assertThat(registry.supportedCatalogIds()).isEqualTo(A2UiCatalogRegistry.shared().supportedCatalogIds());
    }

    @Test
    void ofShouldBuildRegistryFromExplicitSchemaMap() {
        Map<String, Map<String, Map<String, Object>>> schemas = Map.of(
                HOST_CATALOG_ID, Map.of("StatusBadge", Map.of(
                        "type", "object",
                        "properties", Map.of("text", Map.of("type", "string")))));
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.of(schemas);

        assertThat(registry.isSupportedCatalogId(HOST_CATALOG_ID)).isTrue();
        assertThat(registry.isSupportedCatalogId(A2UiCatalogIds.BASIC_V0_9)).isFalse();
        assertThat(registry.catalogRulesText()).isEmpty();
    }
}
