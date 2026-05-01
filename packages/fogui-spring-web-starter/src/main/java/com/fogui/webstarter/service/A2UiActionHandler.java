package com.fogui.webstarter.service;

import com.fogui.contract.a2ui.A2UiMessage;
import com.fogui.contract.a2ui.A2UiUserAction;

import java.util.List;

public interface A2UiActionHandler {

    boolean supports(A2UiUserAction userAction);

    List<A2UiMessage> handle(A2UiUserAction userAction, String requestId);
}