package com.fogui.webstarter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fogui.model.fogui.ContentBlock;
import com.fogui.model.fogui.GenerativeUIResponse;
import com.fogui.model.transform.TransformRequest;
import com.fogui.service.TransformErrorCodes;
import com.fogui.webstarter.prompt.TransformPromptProvider;
import com.fogui.webstarter.runtime.FogUiTransformRuntime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

@DisplayName("TransformService")
class TransformServiceTest {

  private FogUiTransformRuntime transformRuntime;
  private TransformPromptProvider transformPromptProvider;
  private TransformService transformService;

  @BeforeEach
  void setUp() {
    transformRuntime = Mockito.mock(FogUiTransformRuntime.class);
    transformPromptProvider = Mockito.mock(TransformPromptProvider.class);
    transformService = new TransformService(transformRuntime, transformPromptProvider);

    when(transformPromptProvider.createPrompt(anyString(), any()))
        .thenReturn(new Prompt(new SystemMessage("system"), new UserMessage("user")));
  }

  @Test
  @DisplayName(
      "transform should phrase preferred components as canonical component types in the prompt context")
  void transformShouldPhrasePreferredComponentsAsCanonicalComponentTypesInThePromptContext() {
    ChatClient mockClient = Mockito.mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec mockRequestSpec =
        Mockito.mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec mockCallSpec = Mockito.mock(ChatClient.CallResponseSpec.class);
    GenerativeUIResponse uiResponse =
        GenerativeUIResponse.builder().content(List.of(ContentBlock.text("ok"))).build();

    when(transformRuntime.createClient()).thenReturn(mockClient);
    when(transformRuntime.getActiveModelName()).thenReturn("gpt-test");
    when(mockClient.prompt(any(Prompt.class))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.call()).thenReturn(mockCallSpec);
    when(mockCallSpec.entity(GenerativeUIResponse.class)).thenReturn(uiResponse);

    TransformRequest request = new TransformRequest();
    request.setContent("Compare regional sales");

    TransformRequest.TransformContext context = new TransformRequest.TransformContext();
    context.setIntent("dashboard");
    context.setPreferredComponents(List.of("chart", "table"));
    context.setInstructions("Lead with a short summary.");
    request.setContext(context);

    transformService.transform(request, "req-service-1");

    ArgumentCaptor<String> contextHintsCaptor = ArgumentCaptor.forClass(String.class);
    verify(transformPromptProvider).createPrompt(anyString(), contextHintsCaptor.capture());
    String contextHints = contextHintsCaptor.getValue();

    assertTrue(contextHints.contains("Intent: dashboard."));
    assertTrue(
        contextHints.contains(
            "Preferred UI component families (map these to componentType, not the top-level type): chart, table."));
    assertTrue(contextHints.contains("Lead with a short summary."));
  }

  @Test
  @DisplayName("transform should throw parse failure when entity returns null")
  void transformShouldThrowParseFailureWhenEntityReturnsNull() {
    ChatClient mockClient = Mockito.mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec mockRequestSpec =
        Mockito.mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec mockCallSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

    when(transformRuntime.createClient()).thenReturn(mockClient);
    when(mockClient.prompt(any(Prompt.class))).thenReturn(mockRequestSpec);
    when(mockRequestSpec.call()).thenReturn(mockCallSpec);
    when(mockCallSpec.entity(GenerativeUIResponse.class)).thenReturn(null);

    TransformRequest request = new TransformRequest();
    request.setContent("hello");

    TransformExecutionException exception =
        assertThrows(
            TransformExecutionException.class,
            () -> transformService.transform(request, "req-service-1"));

    assertEquals(TransformErrorCodes.TRANSFORM_PARSE_FAILED, exception.getErrorCode());
  }
}
