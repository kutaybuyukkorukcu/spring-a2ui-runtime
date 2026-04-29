package com.fogui.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fogUiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FogUI Showcase Host API")
                        .description(
                                "Showcase host for FogUI deterministic transform, stream, and compatibility APIs.")
                        .version("1.0.0")
                        .license(new License().name("MIT")));
    }
}
