package com.jerokysys.academic.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * La misma configuracion de {@code jerokysys-auth} (clave y claims) permite
 * validar aqui los tokens que alli se emiten: no hace falta codigo compartido.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret,
                                 @Value("${jwt.algorithm}") String algorithmName) {
        MacAlgorithm algorithm = MacAlgorithm.from(algorithmName);
        return NimbusJwtDecoder.withSecretKey(buildKey(secret, algorithm))
                .macAlgorithm(algorithm)
                .build();
    }

    /**
     * Expone el rol del token como autoridad {@code ROLE_<rol>}, de modo que los
     * {@code @PreAuthorize("hasRole(...")} y las reglas por URL funcionen.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${jwt.claim.user}") String userClaim,
            @Value("${jwt.claim.role}") String roleClaim) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(userClaim);
        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            String role = jwt.getClaimAsString(roleClaim);
            if (role == null || role.isBlank()) {
                return List.of();
            }
            return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    /**
     * Deriva la clave HMAC igual que {@code JWTService} de auth: primero intenta
     * Base64 y, si no lo es, usa los bytes UTF-8 tal cual.
     */
    private static SecretKey buildKey(String secret, MacAlgorithm algorithm) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must not be empty");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        int minBits = switch (algorithm) {
            case HS256 -> 256;
            case HS384 -> 384;
            case HS512 -> 512;
        };
        if (bytes.length * 8 < minBits) {
            throw new IllegalStateException("jwt.secret must decode to at least " + minBits
                    + " bits for " + algorithm.getName());
        }
        return new SecretKeySpec(bytes, jcaName(algorithm));
    }

    private static String jcaName(MacAlgorithm algorithm) {
        return switch (algorithm) {
            case HS256 -> "HmacSHA256";
            case HS384 -> "HmacSHA384";
            case HS512 -> "HmacSHA512";
        };
    }
}
