package com.fogui.webstarter.controller;

import com.fogui.contract.a2ui.A2UiCatalogIds;
import com.fogui.webstarter.service.A2UiCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = A2UiCatalogControllerRouteMappingTest.TestApplication.class)
@AutoConfigureMockMvc
@DisplayName("A2UiCatalogController route mappings")
class A2UiCatalogControllerRouteMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private A2UiCatalogService catalogService;

    @BeforeEach
    void setUp() {
        when(catalogService.getCanonicalCatalog()).thenReturn(Map.of("catalogId", A2UiCatalogIds.CANONICAL_V0_8));
    }

    @Test
    void shouldExposeCanonicalCatalogRoute() throws Exception {
        mockMvc.perform(get(A2UiCatalogIds.CANONICAL_V0_8))
                .andExpect(status().isOk());

        verify(catalogService).getCanonicalCatalog();
    }

    @Test
    void shouldRejectFogUiCatalogRoute() throws Exception {
        mockMvc.perform(get("/fogui/catalogs/canonical/v0.8"))
                .andExpect(status().is4xxClientError());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(A2UiCatalogController.class)
    static class TestApplication {
    }
}