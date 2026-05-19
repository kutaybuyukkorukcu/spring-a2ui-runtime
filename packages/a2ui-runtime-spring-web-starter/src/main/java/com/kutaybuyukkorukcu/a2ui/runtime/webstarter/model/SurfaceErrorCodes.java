package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.model;

import com.kutaybuyukkorukcu.a2ui.runtime.error.A2UiErrorCode;

/**
 * @deprecated Use {@link A2UiErrorCode} instead. This class will be removed in a future release.
 */
@Deprecated(forRemoval = true)
public class SurfaceErrorCodes {
    public static final String CONTENT_REQUIRED = A2UiErrorCode.CONTENT_REQUIRED.code();
    public static final String TRANSFORM_FAILED = A2UiErrorCode.TRANSFORM_FAILED.code();
    public static final String TRANSFORM_PARSE_FAILED = A2UiErrorCode.TRANSFORM_PARSE_FAILED.code();
    public static final String NO_COMPATIBLE_CATALOG = A2UiErrorCode.NO_COMPATIBLE_CATALOG.code();
    public static final String A2UI_VALIDATION_FAILED = A2UiErrorCode.A2UI_VALIDATION_FAILED.code();

    private SurfaceErrorCodes() {}
}