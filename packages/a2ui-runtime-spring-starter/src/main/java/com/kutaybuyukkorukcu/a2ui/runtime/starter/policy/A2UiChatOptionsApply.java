package com.kutaybuyukkorukcu.a2ui.runtime.starter.policy;

import org.springframework.lang.Nullable;

import java.util.function.Consumer;

/**
 * Present-only apply rules shared by provider ChatOptions adapters.
 * Null policy fields must not wipe host options; temperature/topP are applied when non-null.
 */
final class A2UiChatOptionsApply {

    private A2UiChatOptionsApply() {
    }

    static <T> void ifPresent(@Nullable T value, Consumer<T> applier) {
        if (value != null) {
            applier.accept(value);
        }
    }

    static void textIfPresent(@Nullable String value, Consumer<String> applier) {
        if (value != null && !value.isBlank()) {
            applier.accept(value);
        }
    }
}
