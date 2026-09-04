package com.Noto_back.repository;

import com.Noto_back.model.RefreshToken;
import com.Noto_back.model.Role;
import com.Noto_back.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;


import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("revokeAllByUserId doit passer revoked à true pour tous les tokens de l'utilisateur")
    void revokeAllByUserId_succes() {
        // Arrange : création de deux utilisateurs
        User user1 = User.builder()
                .username("john_doe")
                .email("john@test.com")
                .password("password123")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .username("jane_doe")
                .email("jane@test.com")
                .password("password123")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);

        // Tokens pour User 1 (revoked = false)
        RefreshToken token1User1 = RefreshToken.builder()
                .token("token_1_user_1")
                .user(user1)
                .expiryDate(LocalDateTime.now().plusSeconds(3600))
                .revoked(false)
                .build();

        RefreshToken token2User1 = RefreshToken.builder()
                .token("token_2_user_1")
                .user(user1)
                .expiryDate(LocalDateTime.now().plusSeconds(3600))
                .revoked(false)
                .build();

        // Token pour User 2 (revoked = false) qui ne doit pas être modifié
        RefreshToken tokenUser2 = RefreshToken.builder()
                .token("token_user_2")
                .user(user2)
                .expiryDate(LocalDateTime.now().plusSeconds(3600))
                .revoked(false)
                .build();

        entityManager.persist(token1User1);
        entityManager.persist(token2User1);
        entityManager.persist(tokenUser2);
        entityManager.flush();

        // Act
        refreshTokenRepository.revokeAllByUserId(user1.getId());

        // Nettoyage du cache du First Level Cache (L1) pour s'assurer de relire en BDD
        entityManager.clear();

        // Assert
        List<RefreshToken> tokensUser1 = refreshTokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser().getId().equals(user1.getId()))
                .toList();

        assertThat(tokensUser1)
                .hasSize(2)
                .allMatch(RefreshToken::isRevoked); // Vérifie que tous les tokens de User 1 sont révoqués

        RefreshToken token2Fetched = refreshTokenRepository.findByToken("token_user_2").orElseThrow();
        assertThat(token2Fetched.isRevoked()).isFalse(); // Vérifie que le token de User 2 n'a pas bougé
    }
}