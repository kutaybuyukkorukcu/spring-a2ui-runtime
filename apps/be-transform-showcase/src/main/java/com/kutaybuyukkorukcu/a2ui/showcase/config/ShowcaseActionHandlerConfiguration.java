package com.kutaybuyukkorukcu.a2ui.showcase.config;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiUserAction;
import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.A2UiActionHandler;
import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShowcaseActionHandlerConfiguration {

    @Bean
    public A2UiActionHandler showcaseConfirmActionHandler() {
        return new A2UiActionHandler() {
            @Override
            public boolean supports(A2UiUserAction userAction) {
                return "main".equals(userAction.surfaceId()) && "confirm".equals(userAction.name());
            }

            @Override
            public List<A2UiMessage> handle(A2UiUserAction userAction, String requestId) {
                return Collections.emptyList();
            }
        };
    }
}