package com.Noto_back.controller;

import com.Noto_back.dto.*;
import com.Noto_back.model.Role;
import com.Noto_back.model.User;
import com.Noto_back.security.UserPrincipal;
import com.Noto_back.service.CustomUserDetailsService;
import com.Noto_back.service.DossierService;
import com.Noto_back.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DossierController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DossierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private DossierService dossierService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private DossierResponse sampleResponse;
    private DossierTreeResponse sampleTreeResponse;

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

        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Correspondance exacte avec DossierResponse(id, nom, description, couleur, parentId, createdAt)
        sampleResponse = new DossierResponse(
                10L,
                "Dossier Test",
                "Description test",
                "#FFFFFF",
                null,
                LocalDateTime.now()
        );

        // Correspondance exacte avec DossierTreeResponse(id, nom, description, couleur, parentId, parentNom, sousDossiers, taches)
        sampleTreeResponse = new DossierTreeResponse(
                10L,
                "Dossier Test",
                "Description test",
                "#FFFFFF",
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("POST /api/dossiers - 201 Created")
    void creerDossier_succes() throws Exception {
        // DossierCreateRequest(nom, description, couleur, parentId)
        DossierCreateRequest request = new DossierCreateRequest("Nouveau Dossier", "Description test", "#FFFFFF", null);

        when(dossierService.creerDossier(eq(1L), any(DossierCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nom").value("Dossier Test"));
    }

    @Test
    @DisplayName("GET /api/dossiers - 200 OK")
    void listerDossierRacines_succes() throws Exception {
        when(dossierService.listerDossiersRacines(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/dossiers/arbre - 200 OK")
    void obtenirArborescence_succes() throws Exception {
        when(dossierService.obtenirArborescence(1L)).thenReturn(List.of(sampleTreeResponse));

        mockMvc.perform(get("/api/dossiers/arbre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/dossiers/{id} - 200 OK")
    void obtenirDossier_succes() throws Exception {
        when(dossierService.obtenirDossier(1L, 10L)).thenReturn(sampleTreeResponse);

        mockMvc.perform(get("/api/dossiers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("PUT /api/dossiers/{id} - 200 OK")
    void modifierDossier_succes() throws Exception {
        // DossierUpdateRequest(nom, description, couleur)
        DossierUpdateRequest request = new DossierUpdateRequest("Nom Modifie", "Description modifiée", "#000000");

        when(dossierService.modifierDossier(eq(1L), eq(10L), any(DossierUpdateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/dossiers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/dossiers/{id}/deplacer - 200 OK")
    void deplacerDossier_succes() throws Exception {
        DossierMoveRequest request = new DossierMoveRequest(2L);

        when(dossierService.deplacerDossier(1L, 10L, 2L)).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/dossiers/10/deplacer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/dossiers/{id} - 204 No Content")
    void supprimerDossier_succes() throws Exception {
        doNothing().when(dossierService).supprimerDossier(1L, 10L);

        mockMvc.perform(delete("/api/dossiers/10"))
                .andExpect(status().isNoContent());
    }
}