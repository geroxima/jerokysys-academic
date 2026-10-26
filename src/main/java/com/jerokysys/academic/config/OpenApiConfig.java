package com.jerokysys.academic.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI academicOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/api/v1")))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Habilita el boton "Authorize" de Swagger para probar los 401/403/2xx.
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
