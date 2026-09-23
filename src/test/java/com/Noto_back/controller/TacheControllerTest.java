package com.Noto_back.controller;


import com.Noto_back.dto.*;
import com.Noto_back.model.Priorite;
import com.Noto_back.model.Role;
import com.Noto_back.model.StatutTache;
import com.Noto_back.model.User;
import com.Noto_back.security.UserPrincipal;
import com.Noto_back.service.CustomUserDetailsService;
import com.Noto_back.service.JwtService;
import com.Noto_back.service.TacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TacheController.class)
@AutoConfigureMockMvc(addFilters = false) // Désactive les filtres de sécurité pour isoler le test du controller
@ActiveProfiles("test")
class TacheControllerTest {

    @Autowired
    private MockMvc mockMvc;


    private ObjectMapper objectMapper;

    @MockitoBean
    private TacheService tacheService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserPrincipal principal;
    private TacheResponse sampleResponse;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@test.com")
                .role(Role.USER)
                .build();

        DossierResponse dossierResponse = new DossierResponse(1L, "dossierTest", "test", "BLEU",null, LocalDateTime.now());
        principal = new UserPrincipal(user);

        // Injecte l'utilisateur authentifié dans le SecurityContext pour @AuthenticationPrincipal
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleResponse = new TacheResponse(
                10L,
                "Titre Tâche",
                "Description",
                StatutTache.A_FAIRE,
                LocalDate.now(),
                Priorite.MOYENNE,
                dossierResponse

        );
    }

    @Nested
    @DisplayName("Routes sous /api/dossiers")
    class DossiersRoutes {

        @Test
        @DisplayName("POST /api/dossiers/{dossierId}/taches - 201 Created")
        void creerTache_succes() throws Exception {
            TacheCreateRequest request = new TacheCreateRequest("Nouvelle Tâche", "Description", null ,Priorite.HAUTE );

            when(tacheService.creerTache(eq(1L), eq(5L), any(TacheCreateRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/dossiers/5/taches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.titre").value("Titre Tâche"));
        }

        @Test
        @DisplayName("GET /api/dossiers/{dossierId}/taches - 200 OK")
        void listerTachesParDossier_succes() throws Exception {
            when(tacheService.listerTachesParDossier(1L, 5L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/dossiers/5/taches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(10));
        }
    }

    @Nested
    @DisplayName("Routes directes /api/taches")
    class DirectRoutes {

        @Test
        @DisplayName("POST /api/taches - 201 Created (Tâche racine)")
        void creerTacheRacine_succes() throws Exception {
            TacheCreateRequest request = new TacheCreateRequest("Tâche Racine", "Desc", null,Priorite.BASSE);

            when(tacheService.creerTacheRacine(eq(1L), any(TacheCreateRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/taches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("GET /api/taches/racine - 200 OK")
        void listerTachesRacines_succes() throws Exception {
            when(tacheService.listerTachesRacines(1L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/taches/racine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("GET /api/taches/{id} - 200 OK")
        void obtenirTache_succes() throws Exception {
            when(tacheService.obtenirTache(1L, 10L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/taches/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("PUT /api/taches/{id} - 200 OK")
        void modifierTache_succes() throws Exception {
            TacheUpdateRequest request = new TacheUpdateRequest("Titre Modifié", "Description", null, Priorite.HAUTE);

            when(tacheService.modifierTache(eq(1L), eq(10L), any(TacheUpdateRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/taches/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PATCH /api/taches/{id}/statut - 200 OK")
        void changerStatut_succes() throws Exception {
            TacheStatutUpdateRequest request = new TacheStatutUpdateRequest(StatutTache.TERMINEE);

            when(tacheService.changerStatut(eq(1L), eq(10L), any(TacheStatutUpdateRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/taches/10/statut")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/taches/{id} - 24 No Content")
        void supprimerTache_succes() throws Exception {
            doNothing().when(tacheService).supprimerTache(1L, 10L);

            mockMvc.perform(delete("/api/taches/10"))
                    .andExpect(status().isNoContent());
        }
    }
}