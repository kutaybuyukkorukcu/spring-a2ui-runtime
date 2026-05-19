package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class A2UiCatalogServiceTest {

    private A2UiCatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new A2UiCatalogService(new ObjectMapper());
    }

    @Test
    void shouldLoadStandardCatalog() {
        Map<String, Object> catalog = catalogService.getStandardCatalog();
        assertThat(catalog).isNotEmpty();
        assertThat(catalog.get("catalogId")).isEqualTo(A2UiCatalogIds.STANDARD_V0_8);
    }

    @Test
    void shouldContainComponentTypes() {
        Map<String, Object> catalog = catalogService.getStandardCatalog();
        assertThat(catalog).containsKey("components");
    }
}