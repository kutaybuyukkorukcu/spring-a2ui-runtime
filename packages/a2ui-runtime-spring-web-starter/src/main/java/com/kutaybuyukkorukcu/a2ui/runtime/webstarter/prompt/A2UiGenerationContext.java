package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public record A2UiGenerationContext(
        String staticPrefix,
        String dynamicSuffix,
        A2UiGenerationContextKey key
) {

    public static final class Builder {
        private final StringBuilder staticPrefix = new StringBuilder();
        private final StringBuilder dynamicSuffix = new StringBuilder();
        private A2UiGenerationContextKey key;
        private boolean staticFrozen;

        public Builder freezeStatic(String cachedPrefix) {
            staticPrefix.setLength(0);
            if (cachedPrefix != null) {
                staticPrefix.append(cachedPrefix);
            }
            staticFrozen = true;
            return this;
        }

        public Builder appendStatic(String text) {
            if (staticFrozen || text == null || text.isBlank()) {
                return this;
            }
            staticPrefix.append(text);
            return this;
        }

        public Builder appendDynamic(String text) {
            if (text == null || text.isBlank()) {
                return this;
            }
            dynamicSuffix.append(text);
            return this;
        }

        public Builder key(A2UiGenerationContextKey key) {
            this.key = key;
            return this;
        }

        public String staticPrefix() {
            return staticPrefix.toString();
        }

        public A2UiGenerationContext build() {
            return new A2UiGenerationContext(staticPrefix.toString(), dynamicSuffix.toString(), key);
        }
    }
}
