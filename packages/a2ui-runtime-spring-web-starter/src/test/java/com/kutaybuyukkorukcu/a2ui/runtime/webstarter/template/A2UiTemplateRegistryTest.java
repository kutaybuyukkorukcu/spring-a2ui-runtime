package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.template;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model.SurfaceExecutionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2UiTemplateRegistryTest {

    @Test
    void noArgConstructorShouldStartEmpty() {
        assertThat(new A2UiTemplateRegistry().templateIds()).isEmpty();
    }

    @Test
    void builderShouldStartEmpty() {
        assertThat(A2UiTemplateRegistry.builder().build().templateIds()).isEmpty();
    }

    @Test
    void shouldLookupRegisteredTemplates() {
        A2UiTemplateRegistry registry = A2UiTemplateRegistry.builder()
                .register(ExampleTextCardTemplate.definition())
                .build();
        assertThat(registry.templateIds()).containsExactly(ExampleTextCardTemplate.ID);
        assertThat(registry.require(ExampleTextCardTemplate.ID).id())
                .isEqualTo(ExampleTextCardTemplate.ID);
    }

    @Test
    void shouldFailFastForUnknownTemplateId() {
        A2UiTemplateRegistry registry = A2UiTemplateRegistry.builder()
                .register(ExampleTextCardTemplate.definition())
                .build();
        assertThatThrownBy(() -> registry.require("unknown-template"))
                .isInstanceOf(SurfaceExecutionException.class)
                .hasMessageContaining("Unknown template id");
    }

    @Test
    void builderShouldRegisterOnlyHostTemplates() {
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
                .register(ExampleTextCardTemplate.definition())
                .register(custom)
                .build();

        assertThat(customRegistry.templateIds()).containsExactlyInAnyOrder(
                ExampleTextCardTemplate.ID, "ops-approval");
        assertThat(customRegistry.require("ops-approval").id()).isEqualTo("ops-approval");
    }
}
