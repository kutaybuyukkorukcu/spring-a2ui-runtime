package com.kutaybuyukkorukcu.a2ui.runtime.protocol;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class A2UiJacksonModule extends SimpleModule {

    public A2UiJacksonModule() {
        super("A2UiModule");
        addDeserializer(A2UiMessage.class, new A2UiMessageDeserializer());
        addSerializer(A2UiMessage.class, new A2UiMessageSerializer());
    }
}