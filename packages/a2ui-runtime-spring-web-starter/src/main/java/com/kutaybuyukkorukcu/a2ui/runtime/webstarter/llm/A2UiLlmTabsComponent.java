package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record A2UiLlmTabsComponent(
        @JsonProperty("tabItems") List<A2UiLlmTabItem> tabItems
) {
}
