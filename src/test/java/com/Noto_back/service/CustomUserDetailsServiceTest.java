package com.Noto_back.service;

import com.Noto_back.model.Role;
import com.Noto_back.model.User;
import com.Noto_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@test.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("Chargement de l'utilisateur par email")
    class LoadUserByUsername {

        @Test
        @DisplayName("retourne un UserDetails quand l'utilisateur est trouvé par son email")
        void loadUserByUsername_succes() {
            // GIVEN
            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

            // WHEN
            UserDetails userDetails = customUserDetailsService.loadUserByUsername("john@test.com");

            // THEN
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("john@test.com");
            assertThat(userDetails.getPassword()).isEqualTo("encoded_pass");
            verify(userRepository).findByEmail("john@test.com");
        }

        @Test
        @DisplayName("lève UsernameNotFoundException quand l'email n'existe pas en base")
        void loadUserByUsername_utilisateurInexistant() {
            // GIVEN
            when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("inconnu@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Utilisateur introuvable : inconnu@test.com");

            verify(userRepository).findByEmail("inconnu@test.com");
        }
    }
}