package com.Noto_back.controller;


import com.Noto_back.dto.TacheCreateRequest;
import com.Noto_back.dto.TacheResponse;
import com.Noto_back.dto.TacheStatutUpdateRequest;
import com.Noto_back.dto.TacheUpdateRequest;
import com.Noto_back.security.UserPrincipal;
import com.Noto_back.service.TacheService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequiredArgsConstructor
@Tag(name="Tâches")
public class TacheController {

    private final TacheService tacheService;

// --- Routes imbriquées sous un dossier ---

    @PostMapping("/api/dossiers/{dossierId}/taches")
    public ResponseEntity<TacheResponse> creerTache(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dossierId,
            @Valid @RequestBody TacheCreateRequest request
    ) {
        TacheResponse response = tacheService.creerTache(principal.getId(), dossierId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/dossiers/{dossierId}/taches")
    public ResponseEntity<List<TacheResponse>> listerTachesParDossier(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dossierId
    ) {
        return ResponseEntity.ok(tacheService.listerTachesParDossier(principal.getId(), dossierId));
    }

    // --- Routes directes sur une tâche ---

    @PostMapping("/api/taches")
    public ResponseEntity<TacheResponse> creerTacheRacine(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TacheCreateRequest request
    ) {
        TacheResponse response = tacheService.creerTacheRacine(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Lister les tâches racines
    @GetMapping("/api/taches/racine")
    public ResponseEntity<List<TacheResponse>> listerTachesRacines(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(tacheService.listerTachesRacines(principal.getId()));
    }

    @GetMapping("/api/taches/{id}")
    public ResponseEntity<TacheResponse> obtenirTache(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(tacheService.obtenirTache(principal.getId(), id));
    }

    @PutMapping("/api/taches/{id}")
    public ResponseEntity<TacheResponse> modifierTache(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TacheUpdateRequest request
    ) {
        return ResponseEntity.ok(tacheService.modifierTache(principal.getId(), id, request));
    }

    @PatchMapping("/api/taches/{id}/statut")
    public ResponseEntity<TacheResponse> changerStatut(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TacheStatutUpdateRequest request
    ) {
        return ResponseEntity.ok(tacheService.changerStatut(principal.getId(), id, request));
    }

    @DeleteMapping("/api/taches/{id}")
    public ResponseEntity<Void> supprimerTache(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        tacheService.supprimerTache(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }







}
