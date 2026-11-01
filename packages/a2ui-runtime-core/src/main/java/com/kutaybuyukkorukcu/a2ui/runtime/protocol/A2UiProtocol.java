package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

public final class A2UiProtocol {

    /** Wire version on every envelope. The schema const is {@code "1.0"}, not {@code "v1.0"}. */
    public static final String SUPPORTED_VERSION = "1.0";

    public static final String A2A_EXTENSION_URI = "https://a2ui.org/a2a-extension/a2ui/v1.0";

    public static final String A2UI_MIME_TYPE = "application/a2ui+json";

    private A2UiProtocol() {
    }

    public static boolean isSupportedVersion(String version) {
        return SUPPORTED_VERSION.equals(version);
    }
}
