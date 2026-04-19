package com.fogui.webstarter.runtime;

import org.springframework.ai.chat.client.ChatClient;

public interface FogUiTransformRuntime {

    ChatClient createClient();

    String getActiveModelName();
}