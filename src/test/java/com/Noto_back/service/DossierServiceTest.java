package com.Noto_back.service;


import com.Noto_back.dto.DossierCreateRequest;
import com.Noto_back.dto.DossierResponse;
import com.Noto_back.dto.DossierTreeResponse;
import com.Noto_back.dto.DossierUpdateRequest;
import com.Noto_back.exceptions.CycleDetectedException;
import com.Noto_back.exceptions.ResourceNotFoundException;
import com.Noto_back.mapper.DossierMapper;
import com.Noto_back.model.*;
import com.Noto_back.repository.DossierRepository;
import com.Noto_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {


    @Mock
    private DossierRepository dossierRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DossierMapper dossierMapper;


    @InjectMocks
    private DossierService dossierService;

    private User user;
    private Dossier dossier;


    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("test").email("test@test.com").build();
        dossier = Dossier.builder().id(10L).nom("Travail").user(user).build();
    }

    @Nested
    @DisplayName("Création d'un dossier")
    class CreerDossier {

        @Test
        @DisplayName("crée un dossier racine quand parentId est null")
        void creerDossierRacine_succes() {
            // GIVEN (arrange) : on prépare le contexte et le comportement des mocks
            DossierCreateRequest request = new DossierCreateRequest("Travail", "desc", "#fff", null);
            DossierResponse expectedResponse = new DossierResponse(10L, "Travail", "desc", "#fff", null, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(dossierRepository.save(any(Dossier.class))).thenReturn(dossier);
            when(dossierMapper.toResponse(dossier)).thenReturn(expectedResponse);

            // WHEN (act) : on exécute la méthode testée
            DossierResponse result = dossierService.creerDossier(1L, request);

            // THEN (assert) : on vérifie le résultat ET les interactions
            assertThat(result.nom()).isEqualTo("Travail");
            verify(dossierRepository, times(1)).save(any(Dossier.class));
            verify(dossierRepository, never()).findByIdAndUserId(any(), any()); // pas de parent -> pas de lookup
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si l'utilisateur n'existe pas")
        void creerDossier_utilisateurInexistant() {
            DossierCreateRequest request = new DossierCreateRequest("Travail", null, null, null);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dossierService.creerDossier(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Utilisateur introuvable");

            verify(dossierRepository, never()).save(any()); // on vérifie qu'on ne sauvegarde JAMAIS en cas d'erreur
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le parent n'existe pas")
        void creerDossier_parentInexistant() {
            DossierCreateRequest request = new DossierCreateRequest("Travail", null, null, (long) 43242);


            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> dossierService.creerDossier(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dossier parent introuvable");

            verify(dossierRepository, never()).save(any()); // on vérifie qu'on ne sauvegarde JAMAIS en cas d'erreur
        }
    }

    @Nested
    @DisplayName("Lecture des dossiers racines")
    class ListerDossiersRacines {

        @Test
        @DisplayName("retourne la liste des dossiers racines pour un utilisateur")
        void listerDossiersRacines_succes() {
            // GIVEN
            DossierResponse response = new DossierResponse(10L, "Travail", "desc", "#fff", null, null);

            when(dossierRepository.findByUserIdAndParentIsNull(1L)).thenReturn(List.of(dossier));
            when(dossierMapper.toResponse(dossier)).thenReturn(response);

            // WHEN
            List<DossierResponse> result = dossierService.listerDossiersRacines(1L);

            // THEN
            assertThat(result).hasSize(1);
            assertThat(result.get(0).nom()).isEqualTo("Travail");
            verify(dossierRepository, times(1)).findByUserIdAndParentIsNull(1L);
            verify(dossierMapper, times(1)).toResponse(dossier);
        }

        @Test
        @DisplayName("retourne une liste vide si aucun dossier racine n'existe")
        void listerDossiersRacines_vide() {
            // GIVEN
            when(dossierRepository.findByUserIdAndParentIsNull(1L)).thenReturn(List.of());

            // WHEN
            List<DossierResponse> result = dossierService.listerDossiersRacines(1L);

            // THEN
            assertThat(result).isEmpty();
            verify(dossierRepository, times(1)).findByUserIdAndParentIsNull(1L);
            verify(dossierMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Obtenir l'arborescence complète")
    class ObtenirArborescence {

        @Test
        @DisplayName("construit l'arbre complet avec sous-dossiers et tâches")
        void obtenirArborescence_succes() {
            // GIVEN : Racine -> Enfant, et Racine contient 1 tâche
            Dossier racine = Dossier.builder().id(10L).nom("Racine").parent(null).taches(new ArrayList<>()).build();
            Dossier enfant = Dossier.builder().id(20L).nom("Enfant").parent(racine).taches(new ArrayList<>()).build();

            // Simule une tâche dans la racine
            Tache tache = Tache.builder()
                    .id(1L)
                    .titre("Tâche 1")
                    .description("Desc")
                    .statut(StatutTache.A_FAIRE)
                    .dateEcheance(null)
                    .priorite(Priorite.HAUTE)
                    .build();
            racine.getTaches().add(tache);

            when(dossierRepository.findByUserId(1L)).thenReturn(List.of(racine, enfant));

            // WHEN
            List<DossierTreeResponse> result = dossierService.obtenirArborescence(1L);

            // THEN
            assertThat(result).hasSize(1);
            DossierTreeResponse racineTree = result.get(0);
            assertThat(racineTree.id()).isEqualTo(10L);
            assertThat(racineTree.taches()).hasSize(1);
            assertThat(racineTree.sousDossiers()).hasSize(1);
            assertThat(racineTree.sousDossiers().get(0).id()).isEqualTo(20L);
            assertThat(racineTree.sousDossiers().get(0).parentNom()).isEqualTo("Racine");
        }
    }

    @Nested
    @DisplayName("Obtenir le détail d'un dossier")
    class ObtenirDossier {

        @Test
        @DisplayName("retourne le détail du dossier avec ses sous-dossiers directs et ses tâches")
        void obtenirDossier_succes() {
            // GIVEN
            Dossier dossierParent = Dossier.builder().id(10L).nom("Parent").taches(new ArrayList<>()).build();
            Dossier sousDossier = Dossier.builder().id(20L).nom("Enfant").build();

            when(dossierRepository.findByIdAndUserIdWithTaches(10L, 1L)).thenReturn(Optional.of(dossierParent));
            when(dossierRepository.findByParentId(10L)).thenReturn(List.of(sousDossier));

            // WHEN
            DossierTreeResponse result = dossierService.obtenirDossier(1L, 10L);

            // THEN
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.sousDossiers()).hasSize(1);
            assertThat(result.sousDossiers().get(0).id()).isEqualTo(20L);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le dossier n'existe pas")
        void obtenirDossier_inexistant() {
            when(dossierRepository.findByIdAndUserIdWithTaches(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dossierService.obtenirDossier(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dossier introuvable");
        }
    }

    @Nested
    @DisplayName("Modification d'un dossier")
    class ModifierDossier {

        @Test
        @DisplayName("modifie le nom, la description et la couleur")
        void modifierDossier_succes() {
            // GIVEN
            DossierUpdateRequest request = new DossierUpdateRequest("Nouveau Nom", "Nouvelle Desc", "#000");
            DossierResponse expectedResponse = new DossierResponse(10L, "Nouveau Nom", "Nouvelle Desc", "#000", null, null);

            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));
            when(dossierMapper.toResponse(dossier)).thenReturn(expectedResponse);

            // WHEN
            DossierResponse result = dossierService.modifierDossier(1L, 10L, request);

            // THEN
            assertThat(result.nom()).isEqualTo("Nouveau Nom");
            verify(dossierRepository, times(1)).save(dossier);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le dossier à modifier n'existe pas")
        void modifierDossier_inexistant() {
            DossierUpdateRequest request = new DossierUpdateRequest("Nouveau Nom", null, null);
            when(dossierRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dossierService.modifierDossier(1L, 99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Déplacement d'un dossier")
    class DeplacerDossier {

        @Test
        @DisplayName("déplace un dossier à la racine quand nouveauParentId est null")
        void deplacerDossier_versRacine() {
            // GIVEN
            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));
            when(dossierMapper.toResponse(dossier)).thenReturn(new DossierResponse(10L, "Travail", null, null, null, null));

            // WHEN
            DossierResponse result = dossierService.deplacerDossier(1L, 10L, null);

            // THEN
            assertThat(dossier.getParent()).isNull();
            verify(dossierMapper).toResponse(dossier);
        }

        @Test
        @DisplayName("déplace un dossier sous un nouveau parent valide")
        void deplacerDossier_versNouveauParent() {
            // GIVEN
            Dossier nouveauParent = Dossier.builder().id(20L).nom("Nouveau Parent").build();

            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));
            when(dossierRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(nouveauParent));

            // WHEN
            dossierService.deplacerDossier(1L, 10L, 20L);

            // THEN
            assertThat(dossier.getParent()).isEqualTo(nouveauParent);
        }

        @Test
        @DisplayName("lève CycleDetectedException quand un dossier tente de devenir son propre parent")
        void deplacerDossier_soiMeme() {
            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));

            assertThatThrownBy(() -> dossierService.deplacerDossier(1L, 10L, 10L))
                    .isInstanceOf(CycleDetectedException.class)
                    .hasMessageContaining("son propre parent");
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le nouveau parent n'existe pas")
        void deplacerDossier_parentInexistant() {
            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));
            when(dossierRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dossierService.deplacerDossier(1L, 10L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dossier parent introuvable");
        }

        @Test
        @DisplayName("lève CycleDetectedException en cas de déplacement dans son propre descendant")
        void deplacerDossier_dansDescendant() {
            // GIVEN : dossier(10L) -> grandEnfant(30L)
            Dossier enfant = Dossier.builder().id(20L).nom("Enfant").parent(dossier).build();
            Dossier grandEnfant = Dossier.builder().id(30L).nom("Grand Enfant").parent(enfant).build();

            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));
            when(dossierRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(grandEnfant));

            // WHEN / THEN
            assertThatThrownBy(() -> dossierService.deplacerDossier(1L, 10L, 30L))
                    .isInstanceOf(CycleDetectedException.class)
                    .hasMessageContaining("propres sous-dossiers");
        }
    }

    @Nested
    @DisplayName("Suppression d'un dossier")
    class SupprimerDossier {

        @Test
        @DisplayName("supprime le dossier quand il existe")
        void supprimerDossier_succes() {
            when(dossierRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(dossier));

            dossierService.supprimerDossier(1L, 10L);

            verify(dossierRepository, times(1)).delete(dossier);
        }

        @Test
        @DisplayName("lève ResourceNotFoundException si le dossier à supprimer n'existe pas")
        void supprimerDossier_inexistant() {
            when(dossierRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dossierService.supprimerDossier(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(dossierRepository, never()).delete(any());
        }
    }

}
