package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Showcase catalog few-shots")
class ShowcaseCatalogContributionTest {

    @Autowired
    private A2UiCatalogRegistry catalogRegistry;

    @Test
    @DisplayName("host examplesText is merged into the catalog registry")
    void shouldRegisterHostButtonSiblingExample() {
        assertThat(catalogRegistry.catalogExamplesText())
                .isEqualTo(ShowcaseCatalogContribution.BUTTON_SIBLING_EXAMPLE.trim());
    }
}
