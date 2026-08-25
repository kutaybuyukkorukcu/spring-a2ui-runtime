package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import java.util.List;
import java.util.Set;

public interface A2UiActionHandler {
    boolean supports(A2UiUserAction userAction);
    List<A2UiMessage> handle(A2UiUserAction userAction, String requestId);

    default Set<String> actionNames() {
        return Set.of();
    }
}