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

    private static final A2UiCatalogRegistry CATALOG_REGISTRY = A2UiCatalogRegistry.shared();
    private static final String DEFAULT_CATALOG_ID = A2UiCatalogIds.STANDARD_V0_8;

    private A2UiRequestCatalogNegotiator() {}

    public static String negotiateCatalogId(A2UiSurfaceRequest request) {
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
                    && CATALOG_REGISTRY.isSupportedCatalogId(supportedCatalogId)) {
                return supportedCatalogId;
            }
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("clientSupportedCatalogIds", supportedCatalogIds);
        details.put("runtimeSupportedCatalogIds", List.copyOf(CATALOG_REGISTRY.supportedCatalogIds()));
        throw new SurfaceExecutionException(
                "Client does not support any catalogs published by this runtime",
                SurfaceErrorCodes.NO_COMPATIBLE_CATALOG, details);
    }
}