package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiRequestCatalogNegotiatorTest {

    @Test
    void shouldReturnDefaultWhenNoCapabilities() {
        String catalogId = A2UiRequestCatalogNegotiator.negotiateCatalogId(
                new A2UiSurfaceRequest("test", null, null));
        assertThat(catalogId).isEqualTo(A2UiCatalogIds.BASIC_V0_9);
    }

    @Test
    void shouldReturnDefaultWhenCapabilitiesNull() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test", null,
                new A2UiSurfaceRequest.ClientCapabilities(null));
        String catalogId = A2UiRequestCatalogNegotiator.negotiateCatalogId(request);
        assertThat(catalogId).isEqualTo(A2UiCatalogIds.BASIC_V0_9);
    }

    @Test
    void shouldReturnMatchingSupportedCatalogId() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test", null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of(A2UiCatalogIds.BASIC_V0_9)));
        String catalogId = A2UiRequestCatalogNegotiator.negotiateCatalogId(request);
        assertThat(catalogId).isEqualTo(A2UiCatalogIds.BASIC_V0_9);
    }

    @Test
    void shouldThrowWhenNoCompatibleCatalog() {
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test", null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of("https://example.com/unknown-catalog")));
        assertThatThrownBy(() -> A2UiRequestCatalogNegotiator.negotiateCatalogId(request))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasFieldOrPropertyWithValue("errorCode", SurfaceErrorCodes.NO_COMPATIBLE_CATALOG);
    }

    @Test
    void instanceShouldNegotiateHostRegisteredCatalog() {
        String hostCatalogId = "https://example.com/catalogs/hitl/1.0";
        A2UiCatalogContribution contribution = new A2UiCatalogContribution() {
            @Override
            public String catalogId() {
                return hostCatalogId;
            }

            @Override
            public Map<String, Map<String, Object>> componentSchemas() {
                return Map.of("StatusBadge", Map.of("type", "object"));
            }
        };
        A2UiCatalogRegistry registry = A2UiCatalogRegistry.withContributions(
                A2UiCatalogRegistry.shared(), List.of(contribution));
        A2UiRequestCatalogNegotiator negotiator = new A2UiRequestCatalogNegotiator(registry);

        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test", null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of(hostCatalogId)));

        assertThat(negotiator.negotiate(request)).isEqualTo(hostCatalogId);
    }

    @Test
    void instanceShouldRejectHostCatalogUnknownToDeprecatedStaticSharedRegistry() {
        String hostCatalogId = "https://example.com/catalogs/hitl/1.0";
        A2UiSurfaceRequest request = new A2UiSurfaceRequest("test", null,
                new A2UiSurfaceRequest.ClientCapabilities(List.of(hostCatalogId)));

        assertThatThrownBy(() -> A2UiRequestCatalogNegotiator.negotiateCatalogId(request))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasFieldOrPropertyWithValue("errorCode", SurfaceErrorCodes.NO_COMPATIBLE_CATALOG);
    }
}
