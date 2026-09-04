package com.Noto_back.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    // Secret HMAC-SHA256 valide (au moins 256 bits / 32 caracteres)
    private final String SECRET_TEST = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtMjU2Yml0cw==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Injection des propriétés @Value
        ReflectionTestUtils.setField(jwtService, "secret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600000L); // 1h
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 86400000L); // 24h

        userDetails = new User("test@domain.com", "password", Collections.emptyList());
    }

    @Nested
    @DisplayName("Génération et Extraction")
    class GenerationEtExtraction {

        @Test
        @DisplayName("génère un access token valide et extrait le username")
        void generateAccessToken_succes() {
            String token = jwtService.generateAccessToken(userDetails);

            assertThat(token).isNotBlank();
            String username = jwtService.extractUsername(token);
            assertThat(username).isEqualTo("test@domain.com");
        }

        @Test
        @DisplayName("génère un refresh token valide")
        void generateRefreshToken_succes() {
            String token = jwtService.generateRefreshToken(userDetails);

            assertThat(token).isNotBlank();
            assertThat(jwtService.extractUsername(token)).isEqualTo("test@domain.com");
        }
    }

    @Nested
    @DisplayName("Validation du Token (isTokenValid)")
    class ValidationToken {

        @Test
        @DisplayName("retourne true pour un token valide correspondant à l'utilisateur")
        void isTokenValid_succes() {
            String token = jwtService.generateAccessToken(userDetails);

            boolean isValid = jwtService.isTokenValid(token, userDetails);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("retourne false si le token appartient à un autre utilisateur")
        void isTokenValid_mauvaisUtilisateur() {
            String token = jwtService.generateAccessToken(userDetails);
            UserDetails autreUser = new User("autre@domain.com", "password", Collections.emptyList());

            boolean isValid = jwtService.isTokenValid(token, autreUser);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("retourne false si le token est expiré")
        void isTokenValid_tokenExpire() {
            // Configuration d'une expiration négative pour simuler un token déjà expiré
            ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);
            String expiredToken = jwtService.generateAccessToken(userDetails);

            boolean isValid = jwtService.isTokenValid(expiredToken, userDetails);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("retourne false en cas d'exception (ex: token corrompu)")
        void isTokenValid_tokenInvalide() {
            boolean isValid = jwtService.isTokenValid("token.invalid.format", userDetails);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation Structurelle (isTokenStructurallyValid)")
    class ValidationStructurelle {

        @Test
        @DisplayName("retourne true si la signature et la structure du token sont valides")
        void isTokenStructurallyValid_succes() {
            String token = jwtService.generateAccessToken(userDetails);

            boolean isValid = jwtService.isTokenStructurallyValid(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("retourne false si le token est malformé ou tronqué")
        void isTokenStructurallyValid_tokenMalforme() {
            boolean isValid = jwtService.isTokenStructurallyValid("invalid_token_str");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("retourne false si le token a été signé avec un autre secret")
        void isTokenStructurallyValid_mauvaiseSignature() {
            String token = jwtService.generateAccessToken(userDetails);

            // On altère la clé de vérification du service
            ReflectionTestUtils.setField(jwtService, "secret", "YW5vdGhlci12ZXJ5LXNlY3VyZS1zZWNyZXQta2V5LTI1NmJpdHM=");

            boolean isValid = jwtService.isTokenStructurallyValid(token);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Cas d'erreur d'extraction")
    class CasErreurExtraction {

        @Test
        @DisplayName("lève une exception lors de l'extraction sur un token invalide")
        void extractClaim_tokenInvalide_lanceException() {
            assertThatThrownBy(() -> jwtService.extractUsername("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }
}