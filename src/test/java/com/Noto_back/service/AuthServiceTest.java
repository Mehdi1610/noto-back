package com.Noto_back.service;

import com.Noto_back.dto.AuthResponse;
import com.Noto_back.dto.LoginRequest;
import com.Noto_back.dto.RefreshRequest;
import com.Noto_back.dto.RegisterRequest;
import com.Noto_back.model.RefreshToken;
import com.Noto_back.model.Role;
import com.Noto_back.model.User;
import com.Noto_back.repository.RefreshTokenRepository;
import com.Noto_back.repository.UserRepository;
import com.Noto_back.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        // Injection de la valeur de configuration @Value
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@test.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("Inscription (Register)")
    class Register {

        @Test
        @DisplayName("inscrit un nouvel utilisateur avec succès et génère les tokens")
        void register_succes() {
            RegisterRequest request = new RegisterRequest("john_doe", "john@test.com", "password123");

            when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
            when(userRepository.existsByUsername("john_doe")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh_token");

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access_token");
            assertThat(response.refreshToken()).isEqualTo("refresh_token");
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("lève IllegalArgumentException si l'email existe déjà")
        void register_emailExisteDeja() {
            RegisterRequest request = new RegisterRequest("john_doe", "john@test.com", "password123");
            when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email est déjà utilisé");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le nom d'utilisateur existe déjà")
        void register_usernameExisteDeja() {
            RegisterRequest request = new RegisterRequest("john_doe", "john@test.com", "password123");
            when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
            when(userRepository.existsByUsername("john_doe")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nom d'utilisateur est déjà pris");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Connexion (Login)")
    class Login {

        @Test
        @DisplayName("authentifie l'utilisateur et retourne les tokens")
        void login_succes() {
            LoginRequest request = new LoginRequest("john@test.com", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh_token");

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access_token");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("lève BadCredentialsException si l'utilisateur est introuvable")
        void login_utilisateurIntrouvable() {
            LoginRequest request = new LoginRequest("inconnu@test.com", "password123");
            when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Email ou mot de passe incorrect");
        }

        @Test
        @DisplayName("lève BadCredentialsException si l'authentification échoue")
        void login_mauvaisMotDePasse() {
            LoginRequest request = new LoginRequest("john@test.com", "wrong_pass");
            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Email ou mot de passe incorrect");
        }
    }

    @Nested
    @DisplayName("Rafraîchissement du token (Refresh)")
    class Refresh {

        @Test
        @DisplayName("régénère les tokens avec un refresh token valide")
        void refresh_succes() {
            RefreshRequest request = new RefreshRequest("valid_refresh_token");
            RefreshToken storedToken = RefreshToken.builder()
                    .token("valid_refresh_token")
                    .user(user)
                    .revoked(false)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();

            when(refreshTokenRepository.findByToken("valid_refresh_token")).thenReturn(Optional.of(storedToken));
            when(jwtService.isTokenStructurallyValid("valid_refresh_token")).thenReturn(true);
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new_access_token");
            when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("new_refresh_token");

            AuthResponse response = authService.refresh(request);

            assertThat(response.accessToken()).isEqualTo("new_access_token");
            assertThat(storedToken.isRevoked()).isTrue();
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // 1 pour révoquer, 1 pour le nouveau token
        }

        @Test
        @DisplayName("lève BadCredentialsException si le refresh token n'existe pas en base")
        void refresh_tokenInexistant() {
            RefreshRequest request = new RefreshRequest("invalid_token");
            when(refreshTokenRepository.findByToken("invalid_token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Refresh token invalide");
        }

        @Test
        @DisplayName("lève BadCredentialsException si le token est révoqué")
        void refresh_tokenRevoque() {
            RefreshRequest request = new RefreshRequest("revoked_token");
            RefreshToken storedToken = RefreshToken.builder()
                    .token("revoked_token")
                    .user(user)
                    .revoked(true)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();

            when(refreshTokenRepository.findByToken("revoked_token")).thenReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("expiré ou révoqué");
        }

        @Test
        @DisplayName("lève BadCredentialsException si le token est expiré")
        void refresh_tokenExpire() {
            RefreshRequest request = new RefreshRequest("expired_token");
            RefreshToken storedToken = RefreshToken.builder()
                    .token("expired_token")
                    .user(user)
                    .revoked(false)
                    .expiryDate(LocalDateTime.now().minusHours(1))
                    .build();

            when(refreshTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("expiré ou révoqué");
        }

        @Test
        @DisplayName("lève BadCredentialsException si le token est structurellement invalide")
        void refresh_tokenStructureInvalide() {
            RefreshRequest request = new RefreshRequest("malformed_token");
            RefreshToken storedToken = RefreshToken.builder()
                    .token("malformed_token")
                    .user(user)
                    .revoked(false)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();

            when(refreshTokenRepository.findByToken("malformed_token")).thenReturn(Optional.of(storedToken));
            when(jwtService.isTokenStructurallyValid("malformed_token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Refresh token invalide");
        }
    }

    @Nested
    @DisplayName("Déconnexion (Logout)")
    class Logout {

        @Test
        @DisplayName("révoque le token si présent en base")
        void logout_succes() {
            RefreshToken storedToken = RefreshToken.builder()
                    .token("token_to_logout")
                    .user(user)
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByToken("token_to_logout")).thenReturn(Optional.of(storedToken));

            authService.logout("token_to_logout");

            assertThat(storedToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(storedToken);
        }

        @Test
        @DisplayName("ne fait rien si le token n'existe pas en base")
        void logout_tokenInexistant() {
            when(refreshTokenRepository.findByToken("unknown_token")).thenReturn(Optional.empty());

            authService.logout("unknown_token");

            verify(refreshTokenRepository, never()).save(any());
        }
    }
}