package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.properties.A2UiWebProperties;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiRuntimeMetrics;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@EnableAutoConfiguration
public class A2UiActionTestConfiguration {

    @Bean
    public ChatModel chatModel() {
        ChatModel mock = Mockito.mock(ChatModel.class);
        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);
        Mockito.when(mock.call(Mockito.any(Prompt.class))).thenReturn(chatResponse);
        return mock;
    }

    @Bean
    public A2UiRuntimeMetrics a2UiRuntimeMetrics() {
        return A2UiRuntimeMetrics.noop();
    }

    @Bean
    public A2UiActionHandler testActionHandler() {
        return new A2UiActionHandler() {
            @Override
            public boolean supports(A2UiUserAction userAction) {
                return true;
            }

            @Override
            public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
                return List.of(new A2UiMessage.SurfaceUpdate(userAction.surfaceId(), List.of()));
            }
        };
    }

    @Bean
    public List<A2UiActionHandler> actionHandlers() {
        return List.of(testActionHandler());
    }
}