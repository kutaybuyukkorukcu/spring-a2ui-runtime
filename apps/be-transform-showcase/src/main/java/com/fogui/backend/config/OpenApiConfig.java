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
                        .title("FogUI Reference Server API")
                        .description(
                                "Reference server for FogUI deterministic transform, stream, and compatibility APIs. "
                                        + "Auth/API-key endpoints are optional reference capabilities.")
                        .version("1.0.0")
                        .license(new License().name("MIT")));
    }
}
