package com.Noto_back.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtAuthentificationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    private JwtAuthentificationEntryPoint entryPoint;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthentificationEntryPoint();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("retourne un statut 401 JSON structuré lors d'un échec d'authentification")
    void commence_renvoieStatus401EtBodyJson() throws Exception {
        // GIVEN
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new BadCredentialsException("Non autorisé");

        // WHEN
        entryPoint.commence(request, response, authException);

        // THEN
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        // Désérialisation pour tester les propriétés du body JSON
        Map<String, Object> body = objectMapper.readValue(
                response.getContentAsString(),
                new TypeReference<>() {}
        );

        assertThat(body).containsEntry("status", 401);
        assertThat(body).containsEntry("message", "Authentification requise ou token invalide/expiré");
        assertThat(body).containsKey("timestamp");
    }
}