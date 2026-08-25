package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.policy;

public interface A2UiActionPolicy {

    boolean requiresConfirmation(String actionName);

    static A2UiActionPolicy none() {
        return actionName -> false;
    }
}
