package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public record A2UiGenerationContext(
        String staticPrefix,
        String dynamicSuffix
) {

    public static final class Builder {
        private final StringBuilder staticPrefix = new StringBuilder();
        private final StringBuilder dynamicSuffix = new StringBuilder();

        public Builder appendStatic(String text) {
            if (text == null || text.isBlank()) {
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

        public A2UiGenerationContext build() {
            return new A2UiGenerationContext(staticPrefix.toString(), dynamicSuffix.toString());
        }
    }
}
