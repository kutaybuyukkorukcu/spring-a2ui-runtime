package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiAction;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiEventName;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import java.util.List;
import java.util.Set;

public interface A2UiActionHandler {
    boolean supports(A2UiAction action);

    List<A2UiMessage> handle(A2UiAction action, String requestId);

    /**
     * Event names this handler admits. Empty means the handler does not contribute names.
     * The union of every handler is the allow-list. An empty union stays fail-open.
     * Function {@code @call} values do not belong here.
     */
    default Set<A2UiEventName> eventNames() {
        return Set.of();
    }
}
