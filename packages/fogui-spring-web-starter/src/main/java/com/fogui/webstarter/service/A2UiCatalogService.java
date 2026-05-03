package com.fogui.webstarter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fogui.contract.a2ui.A2UiCatalogIds;
import com.fogui.contract.a2ui.A2UiCatalogRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public class A2UiCatalogService {

  private final ObjectMapper objectMapper;

  public A2UiCatalogService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> getCanonicalCatalog() {
    ClassPathResource resource = new ClassPathResource(A2UiCatalogRegistry.CANONICAL_CATALOG_RESOURCE);
    try (InputStream inputStream = resource.getInputStream()) {
      Map<String, Object> catalog = objectMapper.readValue(inputStream, new TypeReference<>() {});
      Object catalogId = catalog.get("catalogId");
      if (!A2UiCatalogIds.CANONICAL_V0_8.equals(catalogId)) {
        throw new IllegalStateException("Canonical catalogId does not match published route");
      }
      return catalog;
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load canonical A2UI catalog", ex);
    }
  }
}
