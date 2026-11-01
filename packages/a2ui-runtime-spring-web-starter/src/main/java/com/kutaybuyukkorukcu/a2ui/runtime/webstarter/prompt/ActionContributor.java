package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiAgentFunctionRegistry;
import org.springframework.core.Ordered;

public final class ActionContributor implements A2UiGenerationContextContributor, Ordered {

    private final A2UiActionAllowList actionAllowList;
    private final A2UiAgentFunctionRegistry agentFunctions;

    public ActionContributor(A2UiActionAllowList actionAllowList) {
        this(actionAllowList, null);
    }

    public ActionContributor(A2UiActionAllowList actionAllowList, A2UiAgentFunctionRegistry agentFunctions) {
        this.actionAllowList = actionAllowList == null ? A2UiActionAllowList.empty() : actionAllowList;
        this.agentFunctions = agentFunctions;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
        StringBuilder block = new StringBuilder();
        if (!actionAllowList.isEmpty()) {
            block.append(actionAllowList.formatPlannerBlock()).append("\n\n");
        }
        if (agentFunctions != null) {
            String functions = agentFunctions.formatPlannerBlock();
            if (!functions.isBlank()) {
                block.append(functions).append("\n\n");
            }
        }
        if (block.isEmpty()) {
            return;
        }
        context.appendDynamic(block.toString());
    }
}
