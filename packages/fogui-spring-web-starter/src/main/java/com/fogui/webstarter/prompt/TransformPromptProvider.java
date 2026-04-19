package com.fogui.webstarter.prompt;

import org.springframework.ai.chat.prompt.Prompt;

public interface TransformPromptProvider {

    Prompt createPrompt(String content, String contextHints);
}