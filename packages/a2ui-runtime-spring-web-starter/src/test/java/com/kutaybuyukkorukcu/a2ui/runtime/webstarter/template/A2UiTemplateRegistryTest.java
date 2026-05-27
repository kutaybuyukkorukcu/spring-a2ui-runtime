package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiTemplateRegistryTest {

    private final A2UiTemplateRegistry registry = new A2UiTemplateRegistry();

    @Test
    void shouldLookupRegisteredTemplates() {
        assertThat(registry.templateIds()).containsExactlyInAnyOrder(
                A2UiSurfaceTemplates.TEXT_CARD,
                A2UiSurfaceTemplates.HERO_CTA,
                A2UiSurfaceTemplates.FORM_LOGIN,
                A2UiSurfaceTemplates.WEATHER_CARD);
        assertThat(registry.require(A2UiSurfaceTemplates.TEXT_CARD).id())
                .isEqualTo(A2UiSurfaceTemplates.TEXT_CARD);
    }

    @Test
    void shouldFailFastForUnknownTemplateId() {
        assertThatThrownBy(() -> registry.require("unknown-template"))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("Unknown template id");
    }
}
