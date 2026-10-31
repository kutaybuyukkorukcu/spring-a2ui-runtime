package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2UiIdentityTest {

    private static final String CATALOG =
            "https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json";

    @Test
    void eventNameAndFunctionIdDoNotCompareEqual() {
        A2UiEventName event = new A2UiEventName("openUrl");
        A2UiFunctionId function = new A2UiFunctionId(CATALOG, "openUrl");

        assertThat(event).isNotEqualTo(function);
        assertThat(event.value()).isEqualTo(function.call());
    }

    @Test
    void urlCatalogIdRoundTripsWithoutSplittingOnColon() {
        A2UiFunctionId id = new A2UiFunctionId(CATALOG, "openUrl");

        assertThat(id.catalogId()).isEqualTo(CATALOG);
        assertThat(id.catalogId()).startsWith("https:");
        assertThat(id.call()).isEqualTo("openUrl");
        assertThat(new A2UiFunctionId(id.catalogId(), id.call())).isEqualTo(id);
    }
}
