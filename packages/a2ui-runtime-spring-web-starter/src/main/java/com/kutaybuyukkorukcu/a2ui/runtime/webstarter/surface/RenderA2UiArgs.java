package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import java.util.List;
import java.util.Map;

public record RenderA2UiArgs(
        String surfaceId,
        String root,
        List<Map<String, Object>> components,
        Map<String, Object> data
) {
}
