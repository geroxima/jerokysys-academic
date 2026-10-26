package com.jerokysys.academic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.jerokysys.academic.security.SecurityErrors;

/**
 * Seguridad del microservicio academico: segunda capa de defensa.
 *
 * <p>Aunque el gateway ya valido el token, este servicio vuelve a comprobarlo,
 * de modo que tampoco sirve entrar al puerto 8082 por fuera. Tres niveles:
 *
 * <ol>
 *   <li><b>URL</b>: las reglas de {@code authorizeHttpRequests} de aqui.</li>
 *   <li><b>Metodo</b>: {@code @PreAuthorize} en los servicios, activo gracias a
 *       {@link EnableMethodSecurity}.</li>
 *   <li><b>Firma</b>: la validacion del JWT en {@code JwtConfig}.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtConverter) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, authException) ->
                SecurityErrors.write(response, 401, "UNAUTHORIZED", "Authentication required");
        AccessDeniedHandler deniedHandler = (request, response, accessDeniedException) ->
                SecurityErrors.write(response, 403, "FORBIDDEN", "Access denied");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publicos: documentacion y el manejo de errores de Spring MVC.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/webjars/**", "/error").permitAll()
                        // Escritura: solo ADMIN y PROFESSOR (SECRETARY es solo lectura).
                        .requestMatchers(HttpMethod.POST, "/**").hasAnyRole("ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.PUT, "/**").hasAnyRole("ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.DELETE, "/**").hasAnyRole("ADMIN", "SECRETARY")
                        // Lectura: los tres roles.
                        .requestMatchers(HttpMethod.GET, "/**")
                                .hasAnyRole("ADMIN", "PROFESSOR", "SECRETARY")
                        // Cualquier otro metodo: autenticado.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                // 401 sin token / 403 sin rol, siempre en JSON y sin trazas.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .build();
    }
}
