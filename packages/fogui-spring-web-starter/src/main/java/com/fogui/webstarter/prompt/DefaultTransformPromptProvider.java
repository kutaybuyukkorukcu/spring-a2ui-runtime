package com.fogui.webstarter.prompt;

import com.fogui.service.TransformPrompts;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

public class DefaultTransformPromptProvider implements TransformPromptProvider {

  @Override
  public Prompt createPrompt(String content, String contextHints) {
    return createPrompt(new TransformPromptContext(content, contextHints, null, null));
  }

  @Override
  public Prompt createPrompt(TransformPromptContext promptContext) {
    return new Prompt(
        new SystemMessage(TransformPrompts.TRANSFORM_SYSTEM_PROMPT),
        new UserMessage(TransformPrompts.buildTransformPrompt(promptContext)));
  }
}
