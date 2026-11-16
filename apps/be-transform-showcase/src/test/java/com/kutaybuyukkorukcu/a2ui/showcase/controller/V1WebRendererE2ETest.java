package com.kutaybuyukkorukcu.a2ui.showcase.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Browser check for showcase (:5001) and the Vite demo (:3000).
 * Skipped until a web v1.0 renderer can render a surface and POST an action
 * that validates against renderer_to_agent.json.
 */
@Disabled("""
        As of 2026-11-01, the renderer matrix (https://a2ui.org/reference/renderers/) lists React, Lit, \
        and Angular v1.0 as Planned. The Vite demo depends on @a2ui/react v0.9 and still posts v0.9 action \
        bodies, which this backend rejects with HTTP 400. SwiftUI v1.0 Alpha is not that demo. \
        @a2ui/web_core has a v1.0 path. Issue 3486 (https://github.com/a2ui-project/a2ui/issues/3486) was \
        closed on 2026-10-27 by https://github.com/a2ui-project/a2ui/pull/3665, a web_core RPC payload fix \
        (@call sent as call), not a JSON Schema fix. That change is not in a released @a2ui/web_core \
        (latest 0.14.0). Until a web renderer clears that bar, this test is skipped and the report names \
        this paragraph. @a2ui/react/v0_9 against v1.0 envelopes is not a passing e2e.
        """)
class V1WebRendererE2ETest {

    @Test
    void showcaseAndVitePostAValidV1Action() {
        // Intentionally empty. A passing run would open :5001 and :3000, render a surface,
        // and POST an action that validates against renderer_to_agent.json.
    }
}
