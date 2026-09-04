package com.Noto_back.security;

import com.Noto_back.service.CustomUserDetailsService;
import com.Noto_back.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticatorFilterTest {
    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Traitement du Header Authorization")
    class HeaderProcessing {

        @Test
        @DisplayName("laisse passer sans authentification si le header Authorization est null")
        void doFilterInternal_headerNull() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService, userDetailsService);
        }

        @Test
        @DisplayName("laisse passer sans authentification si le header ne commence pas par 'Bearer '")
        void doFilterInternal_headerSansBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService, userDetailsService);
        }
    }

    @Nested
    @DisplayName("Validation du Token JWT")
    class TokenValidation {

        @Test
        @DisplayName("authentifie l'utilisateur si le token JWT est valide")
        void doFilterInternal_tokenValide_authentifie() throws Exception {
            String token = "valid_token";
            String email = "john@test.com";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("laisse passer la requête sans authentifier si une exception survient lors de l'extraction du username")
        void doFilterInternal_extractionUsernameException() throws Exception {
            String token = "malformed_token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Token invalide"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userDetailsService);
        }

        @Test
        @DisplayName("ne ré-authentifie pas si une authentification existe déjà dans le SecurityContext")
        void doFilterInternal_authentificationExisteDeja() throws Exception {
            String token = "valid_token";
            String email = "john@test.com";

            // Simule un utilisateur déjà authentifié dans le contexte
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("already_auth", null, null)
            );

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(email);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("ne définit pas l'authentification si le token est invalide (isTokenValid = false)")
        void doFilterInternal_tokenInvalide() throws Exception {
            String token = "invalid_token";
            String email = "john@test.com";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }
}
