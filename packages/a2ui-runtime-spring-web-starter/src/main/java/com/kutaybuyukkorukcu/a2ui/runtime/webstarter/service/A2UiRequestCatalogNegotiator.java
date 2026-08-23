package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.A2UiSurfaceRequest;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceErrorCodes;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class A2UiRequestCatalogNegotiator {

    private static final String DEFAULT_CATALOG_ID = A2UiCatalogIds.BASIC_V0_9;

    private final A2UiCatalogRegistry catalogRegistry;
    private final A2UiRuntimeMetrics runtimeMetrics;

    public A2UiRequestCatalogNegotiator(A2UiCatalogRegistry catalogRegistry) {
        this(catalogRegistry, A2UiRuntimeMetrics.noop());
    }

    public A2UiRequestCatalogNegotiator(A2UiCatalogRegistry catalogRegistry, A2UiRuntimeMetrics runtimeMetrics) {
        this.catalogRegistry = catalogRegistry;
        this.runtimeMetrics = runtimeMetrics == null ? A2UiRuntimeMetrics.noop() : runtimeMetrics;
    }

    /** Negotiates against this instance's (possibly host-extended) catalog registry. */
    public String negotiate(A2UiSurfaceRequest request) {
        String catalogId = negotiateCatalogId(request, catalogRegistry);
        runtimeMetrics.recordCatalogSelected(catalogId);
        return catalogId;
    }

    /**
     * @deprecated negotiates only against {@link A2UiCatalogRegistry#shared()} (basic catalog),
     * ignoring any host-registered catalogs. Prefer the injectable {@link A2UiRequestCatalogNegotiator}
     * bean, which negotiates against the runtime's actual (possibly extended) catalog registry.
     */
    @Deprecated
    public static String negotiateCatalogId(A2UiSurfaceRequest request) {
        return negotiateCatalogId(request, A2UiCatalogRegistry.shared());
    }

    private static String negotiateCatalogId(A2UiSurfaceRequest request, A2UiCatalogRegistry catalogRegistry) {
        if (request == null || request.a2uiClientCapabilities() == null) {
            return DEFAULT_CATALOG_ID;
        }

        List<String> supportedCatalogIds = request.a2uiClientCapabilities().supportedCatalogIds();
        if (supportedCatalogIds == null) {
            return DEFAULT_CATALOG_ID;
        }

        for (String supportedCatalogId : supportedCatalogIds) {
            if (supportedCatalogId != null
                    && !supportedCatalogId.isBlank()
                    && catalogRegistry.isSupportedCatalogId(supportedCatalogId)) {
                return supportedCatalogId;
            }
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("clientSupportedCatalogIds", supportedCatalogIds);
        details.put("runtimeSupportedCatalogIds", List.copyOf(catalogRegistry.supportedCatalogIds()));
        throw new SurfaceExecutionException(
                "Client does not support any catalogs published by this runtime",
                SurfaceErrorCodes.NO_COMPATIBLE_CATALOG, details);
    }
}