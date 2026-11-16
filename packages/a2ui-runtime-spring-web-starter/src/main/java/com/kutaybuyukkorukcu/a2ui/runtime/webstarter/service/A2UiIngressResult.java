package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import java.util.List;

public record A2UiIngressResult(int status, Object body) {

    public List<A2UiMessage> messages() {
        if (body instanceof List<?> list && (list.isEmpty() || list.get(0) instanceof A2UiMessage)) {
            @SuppressWarnings("unchecked")
            List<A2UiMessage> messages = (List<A2UiMessage>) list;
            return messages;
        }
        return List.of();
    }

    public static A2UiIngressResult ok(List<A2UiMessage> messages) {
        return new A2UiIngressResult(200, messages == null ? List.of() : List.copyOf(messages));
    }
}
