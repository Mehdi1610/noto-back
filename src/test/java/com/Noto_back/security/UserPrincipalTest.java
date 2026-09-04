package com.Noto_back.security;

import com.Noto_back.model.Role;
import com.Noto_back.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

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

    @Test
    @DisplayName("mappe correctement les propriétés de l'utilisateur")
    void userPrincipal_mappageCorrect() {
        UserPrincipal userPrincipal = new UserPrincipal(user);

        assertThat(userPrincipal.getId()).isEqualTo(1L);
        assertThat(userPrincipal.getEmail()).isEqualTo("john@test.com");
        assertThat(userPrincipal.getUsername()).isEqualTo("john@test.com"); // Vérifie la surcharge pour Spring Security
        assertThat(userPrincipal.getPassword()).isEqualTo("encoded_pass");
    }

    @Test
    @DisplayName("convertit le rôle en authority Spring Security avec le préfixe ROLE_")
    void getAuthorities_generePrefixeRole() {
        UserPrincipal userPrincipal = new UserPrincipal(user);

        assertThat(userPrincipal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("renvoie true pour toutes les propriétés de statut du compte")
    void statusAccount_renvoieTrue() {
        UserPrincipal userPrincipal = new UserPrincipal(user);

        assertThat(userPrincipal.isAccountNonExpired()).isTrue();
        assertThat(userPrincipal.isAccountNonLocked()).isTrue();
        assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
        assertThat(userPrincipal.isEnabled()).isTrue();
    }
}