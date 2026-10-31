package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionId;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiFunctionResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiAgentFunctionRegistryTest {

    private static final String HOST = "https://example.com/catalogs/host/1.0";

    private final A2UiCatalogRegistry catalogs = A2UiCatalogRegistry.withContributions(
            A2UiCatalogRegistry.shared(), List.of(new A2UiCatalogContribution() {
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
                            "quote", Map.of("allowedCallers", "rendererOrAgent"),
                            "lockAccount", Map.of("allowedCallers", "agentOnly"),
                            "formatDate", Map.of("allowedCallers", "rendererOnly"));
                }
            }));

    @Test
    void exactPairResolvesAndOmittedCatalogIdResolvesOnlyOneOwner() {
        A2UiAgentFunction quote = function(HOST, "quote");
        A2UiAgentFunctionRegistry registry = new A2UiAgentFunctionRegistry(List.of(quote), catalogs);

        assertThat(registry.classify(HOST, "quote", catalogs)).isInstanceOf(A2UiAgentFunctionRegistry.Resolved.Ready.class);
        A2UiAgentFunctionRegistry.Resolved omitted = registry.classify(null, "quote", catalogs);
        assertThat(omitted).isInstanceOf(A2UiAgentFunctionRegistry.Resolved.Ready.class);
        assertThat(((A2UiAgentFunctionRegistry.Resolved.Ready) omitted).id().catalogId()).isEqualTo(HOST);
    }

    @Test
    void omittedCatalogIdIsAmbiguousWhenTwoFunctionsShareTheCall() {
        A2UiCatalogRegistry both = A2UiCatalogRegistry.withContributions(catalogs, List.of(new A2UiCatalogContribution() {
            @Override
            public String catalogId() {
                return A2UiCatalogIds.BASIC_V1;
            }

            @Override
            public Map<String, Map<String, Object>> componentSchemas() {
                return Map.of();
            }

            @Override
            public Map<String, Map<String, Object>> functions() {
                return Map.of("quote", Map.of("allowedCallers", "rendererOrAgent"));
            }
        }));
        A2UiAgentFunctionRegistry registry = new A2UiAgentFunctionRegistry(
                List.of(function(HOST, "quote"), function(A2UiCatalogIds.BASIC_V1, "quote")), both);

        assertThat(registry.classify(null, "quote", both)).isInstanceOf(A2UiAgentFunctionRegistry.Resolved.Ambiguous.class);
        assertThat(registry.classify(HOST, "quote", both)).isInstanceOf(A2UiAgentFunctionRegistry.Resolved.Ready.class);
    }

    @Test
    void rendererOnlyAgentOnlyAndDuplicateBeansFailStartup() {
        assertThatThrownBy(() -> new A2UiAgentFunctionRegistry(List.of(function(HOST, "formatDate")), catalogs))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new A2UiAgentFunctionRegistry(List.of(function(HOST, "lockAccount")), catalogs))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new A2UiAgentFunctionRegistry(
                List.of(function(HOST, "quote"), function(HOST, "quote")), catalogs))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new A2UiAgentFunctionRegistry(List.of(function(A2UiCatalogIds.BASIC_V1, "openUrl")), catalogs))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void explicitRendererOnlyCallIsForbidden() {
        A2UiAgentFunctionRegistry registry = new A2UiAgentFunctionRegistry(List.of(), catalogs);

        assertThat(registry.classify(A2UiCatalogIds.BASIC_V1, "openUrl", catalogs))
                .isInstanceOf(A2UiAgentFunctionRegistry.Resolved.Forbidden.class);
        assertThat(registry.classify(null, "openUrl", catalogs))
                .isInstanceOf(A2UiAgentFunctionRegistry.Resolved.Unknown.class);
    }

    private static A2UiAgentFunction function(String catalogId, String call) {
        return new A2UiAgentFunction() {
            @Override
            public A2UiFunctionId id() {
                return new A2UiFunctionId(catalogId, call);
            }

            @Override
            public A2UiFunctionResult invoke(A2UiAgentFunctionRequest request) {
                return new A2UiFunctionResult.Ok(null);
            }
        };
    }
}
