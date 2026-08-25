package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionAllowList;
import org.springframework.core.Ordered;

public final class ActionContributor implements A2UiGenerationContextContributor, Ordered {

    private final A2UiActionAllowList actionAllowList;

    public ActionContributor(A2UiActionAllowList actionAllowList) {
        this.actionAllowList = actionAllowList == null ? A2UiActionAllowList.empty() : actionAllowList;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
        if (actionAllowList.isEmpty()) {
            return;
        }
        context.appendDynamic(actionAllowList.formatPlannerBlock() + "\n\n");
    }
}
