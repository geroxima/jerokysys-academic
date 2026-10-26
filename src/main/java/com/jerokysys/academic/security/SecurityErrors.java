package com.jerokysys.academic.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerokysys.commons.common.Response;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escribe los errores de seguridad (401/403) en el envelope Response de commons.
 * Nunca incluye trazas ni detalles internos del servidor.
 */
public final class SecurityErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SecurityErrors() {
    }

    public static void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        MAPPER.writeValue(response.getWriter(), Response.error(code, message, status));
    }
}
