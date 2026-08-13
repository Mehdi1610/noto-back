package com.Noto_back.controller;

import com.Noto_back.dto.*;
import com.Noto_back.security.UserPrincipal;
import com.Noto_back.service.DossierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name="Dossier")
@AllArgsConstructor
@RequestMapping("api/dossiers")
public class DossierController {

    private final DossierService dossierService;

    @PostMapping
    public ResponseEntity<DossierResponse> creerDossier(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DossierCreateRequest dossierCreateRequest){
        DossierResponse dossierResponse = dossierService.creerDossier(principal.getId(),dossierCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierResponse);
    }

    @GetMapping
    public ResponseEntity<List<DossierResponse>> listerDossierRacines(
            @AuthenticationPrincipal UserPrincipal principal
    ){
        return ResponseEntity.ok(dossierService.listerDossiersRacines(principal.getId()));
    }

    @GetMapping("/arbre")
    public ResponseEntity<List<DossierTreeResponse>> obtenirArborescence(
            @AuthenticationPrincipal UserPrincipal principal
    ){
        return ResponseEntity.ok(dossierService.obtenirArborescence(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DossierTreeResponse> obtenirDossier(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ){
        return ResponseEntity.ok(dossierService.obtenirDossier(principal.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DossierResponse> modifierDossier(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DossierUpdateRequest dossierUpdateRequest
            ){
        return ResponseEntity.ok(dossierService.modifierDossier(principal.getId(), id, dossierUpdateRequest));
    }

    @PutMapping("/{id}/deplacer")
    public ResponseEntity<DossierResponse> deplacerDossier(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DossierMoveRequest dossierMoveRequest
            ){
        return ResponseEntity.ok(dossierService.deplacerDossier(principal.getId(), id, dossierMoveRequest.nouveauParentId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerDossier(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ){
        dossierService.supprimerDossier(userPrincipal.getId(), id);
        return ResponseEntity.noContent().build();
    }

}
