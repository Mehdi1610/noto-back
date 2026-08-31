package com.Noto_back.service;


import com.Noto_back.dto.DossierCreateRequest;
import com.Noto_back.dto.DossierResponse;
import com.Noto_back.exceptions.ResourceNotFoundException;
import com.Noto_back.mapper.DossierMapper;
import com.Noto_back.model.Dossier;
import com.Noto_back.model.User;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DossierServiceTest {


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
    class CreerDossier{

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
    }
}
