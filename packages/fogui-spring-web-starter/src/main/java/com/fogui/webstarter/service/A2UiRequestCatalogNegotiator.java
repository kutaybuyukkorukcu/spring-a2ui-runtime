package com.fogui.webstarter.service;

import com.fogui.contract.a2ui.A2UiCatalogRegistry;
import com.fogui.contract.a2ui.A2UiOutboundMapper;
import com.fogui.model.transform.TransformRequest;
import com.fogui.service.TransformErrorCodes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the best catalog the runtime can use for the current client request.
 */
public final class A2UiRequestCatalogNegotiator {

  private static final A2UiCatalogRegistry CATALOG_REGISTRY = A2UiCatalogRegistry.shared();

  private A2UiRequestCatalogNegotiator() {}

  public static String negotiateCatalogId(TransformRequest request) {
    if (request == null || request.getA2UiClientCapabilities() == null) {
      return A2UiOutboundMapper.DEFAULT_CATALOG_ID;
    }

    List<String> supportedCatalogIds = request.getA2UiClientCapabilities().getSupportedCatalogIds();
    if (supportedCatalogIds == null) {
      return A2UiOutboundMapper.DEFAULT_CATALOG_ID;
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
    throw new TransformExecutionException(
        "Client does not support any catalogs published by this runtime",
        TransformErrorCodes.NO_COMPATIBLE_CATALOG,
        details);
  }
}