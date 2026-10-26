package com.jerokysys.academic.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jerokysys.academic.config.JwtConfig;
import com.jerokysys.academic.config.SecurityConfig;
import com.jerokysys.academic.controllers.DisciplineController;
import com.jerokysys.academic.services.DisciplineService;
import com.jerokysys.dto.DisciplineDTO;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Capas de seguridad de {@code academic}:
 *
 * <ul>
 *   <li>401 sin token (o con token vencido/firmado con otra clave).</li>
 *   <li>403 cuando el rol no llega para el metodo.</li>
 *   <li>2xx cuando el rol alcanza.</li>
 *   <li>{@code @PreAuthorize} activo a nivel de metodo.</li>
 * </ul>
 */
@WebMvcTest(controllers = DisciplineController.class)
@AutoConfigureMockMvc
@Import({DisciplineController.class, SecurityConfig.class, JwtConfig.class,
        AcademicSecurityTest.GuardConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=YWNhZGVtaWMtdGVzdC1zZWNyZXQtMDEyMzQ1Njc4OWFiY2RlZg==",
        "jwt.algorithm=HS256",
        "jwt.claim.user=username",
        "jwt.claim.role=role"
})
class AcademicSecurityTest {

    private static final String SECRET = "YWNhZGVtaWMtdGVzdC1zZWNyZXQtMDEyMzQ1Njc4OWFiY2RlZg==";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisciplineService disciplineService;

    @Autowired
    private Guard guard;

    // ---------------------------------------------------------------- 401

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get("/disciplines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void tokenVencido_responde401() throws Exception {
        mockMvc.perform(get("/disciplines")
                        .header("Authorization", "Bearer " + token("admin", "ADMIN", -60)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenDeOtraClave_responde401() throws Exception {
        mockMvc.perform(get("/disciplines")
                        .header("Authorization", "Bearer " + forgedToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ---------------------------------------------------------------- 403

    @Test
    void secretariaNoPuedeEscribir_responde403() throws Exception {
        mockMvc.perform(post("/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token("secre", "SECRETARY"))
                        .content("{\"name\":\"Hip Hop\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void tokenSinRol_responde403() throws Exception {
        mockMvc.perform(get("/disciplines")
                        .header("Authorization", "Bearer " + tokenWithoutRole()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ---------------------------------------------------------------- 2xx

    @Test
    void profesorPuedeEscribir_responde201() throws Exception {
        when(disciplineService.create(any(DisciplineDTO.class)))
                .thenReturn(new DisciplineDTO("Hip Hop"));

        mockMvc.perform(post("/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token("prof", "PROFESSOR"))
                        .content("{\"name\":\"Hip Hop\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Hip Hop"));
    }

    @Test
    void secretariaPuedeLeer_responde200() throws Exception {
        mockMvc.perform(get("/disciplines")
                        .header("Authorization", "Bearer " + token("secre", "SECRETARY")))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------- nivel de metodo

    @Test
    @WithMockUser(authorities = "ROLE_SECRETARY")
    void metodoDeLectura_permiteSecretaria() {
        assertThat(guard.read()).isEqualTo("read");
    }

    @Test
    @WithMockUser(authorities = "ROLE_SECRETARY")
    void metodoDeEscritura_deniegaSecretaria() {
        assertThatThrownBy(() -> guard.write())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ROLE_PROFESSOR")
    void metodoDeEscritura_permiteProfesor() {
        assertThat(guard.write()).isEqualTo("write");
    }


    // ------------------------------------------------------------ helpers

    private static String token(String username, String role) {
        return token(username, role, 60);
    }

    private static String token(String username, String role, int lifetimeSeconds) {
        try {
            long now = System.currentTimeMillis();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("Test token")
                    .claim("username", username)
                    .claim("role", role)
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + lifetimeSeconds * 1000L))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(Base64.getDecoder().decode(SECRET)));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Token valido pero sin claim de rol: no tiene ninguna autoridad. */
    private static String tokenWithoutRole() {
        try {
            long now = System.currentTimeMillis();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("Test token")
                    .claim("username", "sinrol")
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + 60_000L))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(Base64.getDecoder().decode(SECRET)));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Token firmado con otra clave: la firma no valida. */
    private static String forgedToken() {        try {
            long now = System.currentTimeMillis();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("Test token")
                    .claim("username", "hacker")
                    .claim("role", "ADMIN")
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + 60_000L))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner("otra-clave-completamente-distinta-00".getBytes()));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Bean minimo con {@code @PreAuthorize} para comprobar que
     * {@code @EnableMethodSecurity} esta activo en este contexto.
     */
    @Configuration(proxyBeanMethods = false)
    static class GuardConfig {

        @Bean
        Guard guard() {
            return new Guard();
        }
    }

    public static class Guard {

        @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR','SECRETARY')")
        public String read() {
            return "read";
        }

        @PreAuthorize("hasAnyRole('ADMIN','PROFESSOR')")
        public String write() {
            return "write";
        }
    }
}
