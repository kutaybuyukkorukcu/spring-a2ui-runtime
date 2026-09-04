package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogContribution;
import com.kutaybuyukkorukcu.a2ui.runtime.catalog.A2UiCatalogIds;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Host few-shots for the vendored basic catalog. The runtime ships none.
 */
@Component
public final class ShowcaseCatalogContribution implements A2UiCatalogContribution {

    static final String BUTTON_SIBLING_EXAMPLE = """
            Button.child is a sibling component id in the same components array — not the label string, not an inline object.

            Valid renderA2Ui components (every child id is also an entry):
            [
              {"id":"root","component":"Column","children":["notesField","submitBtn"]},
              {"id":"notesField","component":"TextField","label":"Notes","value":{"path":"/notes"}},
              {"id":"submitBtn","component":"Button","child":"submitBtnText","action":{"event":{"name":"submit_change","context":{"notes":{"path":"/notes"}}}}},
              {"id":"submitBtnText","component":"Text","text":"Submit for review"}
            ]

            Invalid: a Button whose child id is missing from that array (unknown child id).
            """;

    @Override
    public String catalogId() {
        return A2UiCatalogIds.BASIC_V0_9;
    }

    @Override
    public Map<String, Map<String, Object>> componentSchemas() {
        return Map.of();
    }

    @Override
    public String examplesText() {
        return BUTTON_SIBLING_EXAMPLE;
    }
}
