package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

    @Test
    void builderWithBootstrapDefaultsShouldMatchNoArgConstructor() {
        A2UiTemplateRegistry fromBuilder = A2UiTemplateRegistry.builder().withBootstrapDefaults().build();
        assertThat(fromBuilder.templateIds()).isEqualTo(registry.templateIds());
    }

    @Test
    void builderShouldRegisterCustomTemplateAlongsideBootstrapDefaults() {
        A2UiTemplateDefinition custom = new A2UiTemplateDefinition(
                "ops-approval",
                "Ops approval card",
                Set.of("summary"),
                Set.of(),
                () -> A2UiFixedSurfaceSpec.builder("ops-approval", "root")
                        .requiredSlots("summary")
                        .components(() -> List.of())
                        .build());

        A2UiTemplateRegistry customRegistry = A2UiTemplateRegistry.builder()
                .withBootstrapDefaults()
                .register(custom)
                .build();

        assertThat(customRegistry.templateIds()).contains(
                "ops-approval",
                A2UiSurfaceTemplates.TEXT_CARD,
                A2UiSurfaceTemplates.HERO_CTA,
                A2UiSurfaceTemplates.FORM_LOGIN,
                A2UiSurfaceTemplates.WEATHER_CARD);
        assertThat(customRegistry.require("ops-approval").id()).isEqualTo("ops-approval");
    }

    @Test
    void builderShouldFailFastForUnknownTemplateId() {
        A2UiTemplateRegistry customRegistry = A2UiTemplateRegistry.builder().withBootstrapDefaults().build();
        assertThatThrownBy(() -> customRegistry.require("unknown-template"))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("Unknown template id");
    }

    @Test
    void builderWithoutBootstrapDefaultsShouldOnlyContainRegisteredTemplates() {
        A2UiTemplateDefinition custom = new A2UiTemplateDefinition(
                "only-custom",
                "Only custom template",
                Set.of(),
                Set.of(),
                () -> A2UiFixedSurfaceSpec.builder("only-custom", "root")
                        .components(() -> List.of())
                        .build());

        A2UiTemplateRegistry customOnlyRegistry = A2UiTemplateRegistry.builder().register(custom).build();

        assertThat(customOnlyRegistry.templateIds()).containsExactly("only-custom");
    }
}
