package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;

/**
 * @deprecated Use {@link A2UiErrorCode} instead. This class will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class A2UiActionErrorCodes {
    public static final String INVALID_CLIENT_EVENT = A2UiErrorCode.INVALID_CLIENT_EVENT.code();
    public static final String INVALID_USER_ACTION = A2UiErrorCode.INVALID_USER_ACTION.code();
    public static final String ACTION_NOT_HANDLED = A2UiErrorCode.ACTION_NOT_HANDLED.code();
    public static final String INVALID_ACTION_RESPONSE = A2UiErrorCode.INVALID_ACTION_RESPONSE.code();

    private A2UiActionErrorCodes() {}
}