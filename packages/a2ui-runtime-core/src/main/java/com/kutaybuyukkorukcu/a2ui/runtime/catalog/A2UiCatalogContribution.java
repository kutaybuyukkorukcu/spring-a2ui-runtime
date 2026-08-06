package com.kutaybuyukkorukcu.a2ui.runtime.catalog;

import java.util.Map;

/**
 * Host SPI for registering an A2UI catalog (or extending an existing one) with the runtime.
 * <p>
 * Hosts author their own catalog schema (types + props) to match their design system and FE
 * renderers ({@code https://a2ui.org/guides/defining-your-own-catalog/}); spring-a2ui registers
 * it for server-side validation, tool-schema generation, and dynamic-mode prompts. We do not
 * ship a component kit or catalog marketplace — this is the same altitude as {@code A2UiActionHandler}.
 */
public interface A2UiCatalogContribution {

    /** The A2UI catalog id this contribution targets (new id, or an existing one to extend). */
    String catalogId();

    /**
     * Component type name to flattened JSON Schema object ({@code properties}, {@code required},
     * {@code additionalProperties}, ...), matching the shape returned by
     * {@link A2UiCatalogRegistry#componentSchema(String, String)}.
     */
    Map<String, Map<String, Object>> componentSchemas();

    /** Optional catalog authoring rules appended to the runtime's catalog rules text. */
    default String rulesText() {
        return "";
    }
}
