package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = A2UiTestConfiguration.class)
class A2UiContextLoadIntegrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void shouldLoadApplicationContext() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void shouldRegisterA2UiWebAutoConfiguration() {
        assertThat(applicationContext.containsBean("a2UiSurfaceService")).isTrue();
        assertThat(applicationContext.containsBean("a2UiStreamController")).isTrue();
        assertThat(applicationContext.containsBean("a2UiCatalogController")).isTrue();
        assertThat(applicationContext.containsBean("a2UiActionController")).isTrue();
    }

    @Test
    void shouldRegisterJacksonModule() {
        assertThat(applicationContext.containsBean("a2UiJacksonModule")).isTrue();
    }

    @Test
    void shouldRegisterStarterBeans() {
        assertThat(applicationContext.containsBean("a2UiCatalogRegistry")).isTrue();
        assertThat(applicationContext.containsBean("a2UiMessageParser")).isTrue();
        assertThat(applicationContext.containsBean("a2UiMessageValidator")).isTrue();
    }
}