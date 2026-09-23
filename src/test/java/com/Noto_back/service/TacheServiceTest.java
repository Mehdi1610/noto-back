package com.Noto_back.service;

import com.Noto_back.dto.*;
import com.Noto_back.exceptions.ResourceNotFoundException;
import com.Noto_back.mapper.TacheMapper;
import com.Noto_back.model.Dossier;
import com.Noto_back.model.Priorite;
import com.Noto_back.model.StatutTache;
import com.Noto_back.model.Tache;
import com.Noto_back.model.User;
import com.Noto_back.repository.DossierRepository;
import com.Noto_back.repository.TacheRepository;
import com.Noto_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacheServiceTest {

    @Mock
    private TacheRepository tacheRepository;

    @Mock
    private DossierRepository dossierRepository;

    @Mock
    private TacheMapper tacheMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TacheService tacheService;

    private User user;
    private Dossier dossier;
    private Tache tache;
    private TacheResponse tacheResponse;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("test").email("test@test.com").build();
        dossier = Dossier.builder().id(10L).nom("Dossier Test").user(user).build();

        tache = Tache.builder()
                .id(100L)
                .titre("Tâche Test")
                .description("Description")
                .statut(StatutTache.A_FAIRE)
                .priorite(Priorite.MOYENNE)
                .user(user)
                .dossier(dossier)
                .build();

        DossierResponse dossierResponse = new DossierResponse(1L, "dossierTest", "test", "BLEU",null, LocalDateTime.now());

        tacheResponse = new TacheResponse(100L, "Tâche Test", "Description", StatutTache.A_FAIRE, null, Priorite.MOYENNE, dossierResponse);
    }

    @Nested
    @DisplayName("Création d'une tâche")
    class CreerTache {

        @Test
        @DisplayName("crée une tâche dans un dossier existant")
        void creerTache_dansDossier_succes() {
            TacheCreateRequest request = new TacheCreateRequest("Tâche Test", "Description", null, Priorite.MOYENNE);

            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tacheRepository.save(any(Tache.class))).thenReturn(tache);
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            TacheResponse result = tacheService.creerTache(1L, 10L, request);

            assertThat(result.titre()).isEqualTo("Tâche Test");
            verify(tacheRepository, times(1)).save(any(Tache.class));
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le dossier n'existe pas")
        void creerTache_dossierInexistant() {
            TacheCreateRequest request = new TacheCreateRequest("Tâche Test", null, null, Priorite.MOYENNE);
            when(dossierRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.creerTache(1L, 99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dossier introuvable");

            verify(tacheRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si l'utilisateur n'existe pas lors de la création dans un dossier")
        void creerTache_utilisateurInexistant() {
            TacheCreateRequest request = new TacheCreateRequest("Tâche Test", null, null, Priorite.MOYENNE);
            when(dossierRepository.findByIdAndUserId(10L, 99L)).thenReturn(Optional.of(dossier));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.creerTache(99L, 10L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Utilisateur introuvable");

            verify(tacheRepository, never()).save(any());
        }

        @Test
        @DisplayName("crée une tâche racine sans dossier")
        void creerTacheRacine_succes() {
            TacheCreateRequest request = new TacheCreateRequest("Tâche Racine", "Description", null, Priorite.HAUTE);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tacheRepository.save(any(Tache.class))).thenReturn(tache);
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            TacheResponse result = tacheService.creerTacheRacine(1L, request);

            assertThat(result).isNotNull();
            verify(tacheRepository, times(1)).save(any(Tache.class));
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si l'utilisateur n'existe pas lors de la création racine")
        void creerTacheRacine_utilisateurInexistant() {
            TacheCreateRequest request = new TacheCreateRequest("Tâche Racine", null, null, Priorite.HAUTE);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.creerTacheRacine(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Utilisateur introuvable");

            verify(tacheRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Lecture des tâches")
    class LectureTaches {

        @Test
        @DisplayName("liste les tâches racines")
        void listerTachesRacines_succes() {
            when(tacheRepository.findByUserIdAndDossierIsNull(1L)).thenReturn(List.of(tache));
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            List<TacheResponse> result = tacheService.listerTachesRacines(1L);

            assertThat(result).hasSize(1);
            verify(tacheRepository).findByUserIdAndDossierIsNull(1L);
        }

        @Test
        @DisplayName("liste les tâches d'un dossier")
        void listerTachesParDossier_succes() {
            when(dossierRepository.existsByIdAndUserId(10L, 1L)).thenReturn(true);
            when(tacheRepository.findByDossierId(10L)).thenReturn(List.of(tache));
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            List<TacheResponse> result = tacheService.listerTachesParDossier(1L, 10L);

            assertThat(result).hasSize(1);
            verify(tacheRepository).findByDossierId(10L);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le dossier n'existe pas lors du listing")
        void listerTachesParDossier_dossierInexistant() {
            when(dossierRepository.existsByIdAndUserId(99L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> tacheService.listerTachesParDossier(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dossier introuvable");

            verify(tacheRepository, never()).findByDossierId(any());
        }

        @Test
        @DisplayName("obtient une tâche précise par son ID et userID")
        void obtenirTache_succes() {
            when(tacheRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(tache));
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            TacheResponse result = tacheService.obtenirTache(1L, 100L);

            assertThat(result.id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si la tâche est introuvable")
        void obtenirTache_inexistante() {
            when(tacheRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.obtenirTache(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Tâche introuvable");
        }
    }

    @Nested
    @DisplayName("Mise à jour des tâches")
    class ModificationTaches {

        @Test
        @DisplayName("modifie une tâche complète")
        void modifierTache_succes() {
            TacheUpdateRequest request = new TacheUpdateRequest("Titre Modifié", "Nouveau", LocalDate.now(), Priorite.HAUTE);

            when(tacheRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(tache));
            when(tacheRepository.save(tache)).thenReturn(tache);
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            TacheResponse result = tacheService.modifierTache(1L, 100L, request);

            assertThat(result).isNotNull();
            verify(tacheRepository).save(tache);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si la tâche à modifier n'existe pas")
        void modifierTache_inexistante() {
            TacheUpdateRequest request = new TacheUpdateRequest("Titre", "Desc", null, Priorite.BASSE);
            when(tacheRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.modifierTache(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("change le statut d'une tâche")
        void changerStatut_succes() {
            TacheStatutUpdateRequest request = new TacheStatutUpdateRequest(StatutTache.TERMINEE);

            when(tacheRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(tache));
            when(tacheRepository.save(tache)).thenReturn(tache);
            when(tacheMapper.toResponse(tache)).thenReturn(tacheResponse);

            TacheResponse result = tacheService.changerStatut(1L, 100L, request);

            assertThat(result).isNotNull();
            assertThat(tache.getStatut()).isEqualTo(StatutTache.TERMINEE);
            verify(tacheRepository).save(tache);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si la tâche pour changement de statut n'existe pas")
        void changerStatut_inexistante() {
            TacheStatutUpdateRequest request = new TacheStatutUpdateRequest(StatutTache.EN_COURS);
            when(tacheRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.changerStatut(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Suppression des tâches")
    class SuppressionTaches {

        @Test
        @DisplayName("supprime la tâche quand elle existe")
        void supprimerTache_succes() {
            when(tacheRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(tache));

            tacheService.supprimerTache(1L, 100L);

            verify(tacheRepository, times(1)).delete(tache);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si la tâche à supprimer n'existe pas")
        void supprimerTache_inexistante() {
            when(tacheRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tacheService.supprimerTache(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(tacheRepository, never()).delete(any());
        }
    }
}