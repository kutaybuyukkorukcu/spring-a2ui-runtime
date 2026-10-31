package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Browser check for showcase (:5001) and the Vite demo (:3000).
 * Skipped until a web v1.0 renderer can render a surface and POST an action
 * that validates against renderer_to_agent.json.
 */
@Disabled("""
        As of 2026-10-31, the renderer matrix (https://a2ui.org/reference/renderers/) lists React, Lit, \
        and Angular v1.0 as Planned. The Vite demo depends on @a2ui/react. SwiftUI v1.0 Alpha is not that demo. \
        @a2ui/web_core has a v1.0 path and issue 3486 (https://github.com/a2ui-project/a2ui/issues/3486) tracks \
        @call serialized as call. Until a web renderer clears that bar, this test is skipped and the report names \
        this paragraph. @a2ui/react/v0_9 against v1.0 envelopes is not a passing e2e.
        """)
class V1WebRendererE2ETest {

    @Test
    void showcaseAndVitePostAValidV1Action() {
        // Intentionally empty. A passing run would open :5001 and :3000, render a surface,
        // and POST an action that validates against renderer_to_agent.json.
    }
}
