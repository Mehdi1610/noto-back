package com.Noto_back.repository;

import com.Noto_back.model.Dossier;
import com.Noto_back.model.Role;
import com.Noto_back.model.StatutTache;
import com.Noto_back.model.Tache;
import com.Noto_back.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DossierRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DossierRepository dossierRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .username("john_doe")
                .email("john@test.com")
                .password("password123")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        user2 = User.builder()
                .username("jane_doe")
                .email("jane@test.com")
                .password("password123")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persist(user1);
        entityManager.persist(user2);
    }

    @Test
    @DisplayName("findByUserIdAndParentIsNull - Doit retourner uniquement les dossiers racines de l'utilisateur")
    void findByUserIdAndParentIsNull_succes() {
        Dossier racineUser1 = Dossier.builder().nom("Racine U1").user(user1).build();
        Dossier enfantUser1 = Dossier.builder().nom("Enfant U1").user(user1).parent(racineUser1).build();
        Dossier racineUser2 = Dossier.builder().nom("Racine U2").user(user2).build();

        entityManager.persist(racineUser1);
        entityManager.persist(enfantUser1);
        entityManager.persist(racineUser2);
        entityManager.flush();

        List<Dossier> resultats = dossierRepository.findByUserIdAndParentIsNull(user1.getId());

        assertThat(resultats).hasSize(1);
        assertThat(resultats.get(0).getNom()).isEqualTo("Racine U1");
    }

    @Test
    @DisplayName("findByParentId - Doit retourner les sous-dossiers directs")
    void findByParentId_succes() {
        Dossier racine = Dossier.builder().nom("Racine").user(user1).build();
        Dossier enfant1 = Dossier.builder().nom("Enfant 1").user(user1).parent(racine).build();
        Dossier enfant2 = Dossier.builder().nom("Enfant 2").user(user1).parent(racine).build();

        entityManager.persist(racine);
        entityManager.persist(enfant1);
        entityManager.persist(enfant2);
        entityManager.flush();

        List<Dossier> resultats = dossierRepository.findByParentId(racine.getId());

        assertThat(resultats).hasSize(2);
        assertThat(resultats).extracting(Dossier::getNom).containsExactlyInAnyOrder("Enfant 1", "Enfant 2");
    }

    @Test
    @DisplayName("findByIdAndUserIdWithTaches - Doit charger le dossier avec ses tâches associées (JOIN FETCH)")
    void findByIdAndUserIdWithTaches_succes() {
        Dossier dossier = Dossier.builder().nom("Dossier Projet").user(user1).build();
        entityManager.persist(dossier);

        Tache tache1 = Tache.builder().titre("Tâche 1").statut(StatutTache.A_FAIRE).dossier(dossier).user(user1).build();
        Tache tache2 = Tache.builder().titre("Tâche 2").statut(StatutTache.EN_COURS).dossier(dossier).user(user1).build();

        entityManager.persist(tache1);
        entityManager.persist(tache2);
        entityManager.flush();
        entityManager.clear(); // Vide la mémoire pour forcer le chargement depuis la BDD

        Optional<Dossier> resultat = dossierRepository.findByIdAndUserIdWithTaches(dossier.getId(), user1.getId());

        assertThat(resultat).isPresent();
        assertThat(resultat.get().getNom()).isEqualTo("Dossier Projet");
        assertThat(resultat.get().getTaches()).hasSize(2);
    }

    @Test
    @DisplayName("findByIdAndUserIdWithTaches - Ne doit rien retourner si le dossier appartient à un autre utilisateur")
    void findByIdAndUserIdWithTaches_mauvaisUtilisateur() {
        Dossier dossier = Dossier.builder().nom("Dossier Sécurisé").user(user1).build();
        entityManager.persist(dossier);
        entityManager.flush();

        Optional<Dossier> resultat = dossierRepository.findByIdAndUserIdWithTaches(dossier.getId(), user2.getId());

        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("existsByIdAndUserId - Doit vérifier l'appartenance du dossier")
    void existsByIdAndUserId_succes() {
        Dossier dossier = Dossier.builder().nom("Dossier Test").user(user1).build();
        entityManager.persist(dossier);
        entityManager.flush();

        boolean existePourUser1 = dossierRepository.existsByIdAndUserId(dossier.getId(), user1.getId());
        boolean existePourUser2 = dossierRepository.existsByIdAndUserId(dossier.getId(), user2.getId());

        assertThat(existePourUser1).isTrue();
        assertThat(existePourUser2).isFalse();
    }
}