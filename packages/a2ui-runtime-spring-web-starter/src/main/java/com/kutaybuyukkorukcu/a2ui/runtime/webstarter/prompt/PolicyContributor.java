package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy.A2UiSurfacePolicy;
import org.springframework.core.Ordered;

public final class PolicyContributor implements A2UiGenerationContextContributor, Ordered {

    private final A2UiSurfacePolicy surfacePolicy;

    public PolicyContributor(A2UiSurfacePolicy surfacePolicy) {
        this.surfacePolicy = surfacePolicy == null ? A2UiSurfacePolicy.none() : surfacePolicy;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    @Override
    public boolean contributesStatic() {
        return false;
    }

    @Override
    public void contribute(A2UiGenerationRequest request, A2UiGenerationContext.Builder context) {
        String block = surfacePolicy.formatPlannerBlock();
        if (block.isEmpty()) {
            return;
        }
        context.appendDynamic(block + "\n\n");
    }
}
