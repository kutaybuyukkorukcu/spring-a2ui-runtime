package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.runtime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.List;

/**
 * Shared ChatClient clone + advisor attach for compose hop 1 and dynamic hop 2.
 *
 * @apiNote internal — not a host SPI.
 */
public final class ChatClientFactories {

    private ChatClientFactories() {
    }

    public static ChatClient cloneWithAdvisors(ChatClient.Builder chatClientBuilder, List<Advisor> advisors) {
        ChatClient.Builder builder = chatClientBuilder.clone();
        if (advisors != null) {
            for (Advisor advisor : advisors) {
                builder = builder.defaultAdvisors(advisor);
            }
        }
        return builder.build();
    }
}
