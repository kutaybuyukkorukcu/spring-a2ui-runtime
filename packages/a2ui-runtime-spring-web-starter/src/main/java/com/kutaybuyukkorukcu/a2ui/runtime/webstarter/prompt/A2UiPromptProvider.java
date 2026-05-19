package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.prompt;

public interface A2UiPromptProvider {
    String createSystemPrompt(A2UiPromptContext context);
    String createUserPrompt(A2UiPromptContext context);
}