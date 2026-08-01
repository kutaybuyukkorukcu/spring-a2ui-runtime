package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

public final class A2UiProtocol {

    /** Wire version emitted on every envelope. Schemas also accept {@code "v0.9"}. */
    public static final String SUPPORTED_VERSION = "v0.9.1";

    public static final String A2A_EXTENSION_URI = "https://a2ui.org/a2a-extension/a2ui/v0.9.1";

    public static final String A2UI_MIME_TYPE = "application/a2ui+json";

    private A2UiProtocol() {
    }

    public static boolean isSupportedVersion(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        return SUPPORTED_VERSION.equals(version) || "v0.9".equals(version);
    }
}
