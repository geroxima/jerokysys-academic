package com.jerokysys.academic.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI academicOpenAPI() {
        return new OpenAPI().servers(List.of(new Server().url("/api/v1")));
    }

}
