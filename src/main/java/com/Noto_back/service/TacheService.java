package com.Noto_back.service;

import com.Noto_back.dto.TacheCreateRequest;
import com.Noto_back.dto.TacheResponse;
import com.Noto_back.dto.TacheStatutUpdateRequest;
import com.Noto_back.dto.TacheUpdateRequest;
import com.Noto_back.exceptions.ResourceNotFoundException;
import com.Noto_back.mapper.TacheMapper;
import com.Noto_back.model.Dossier;
import com.Noto_back.model.Tache;
import com.Noto_back.repository.DossierRepository;
import com.Noto_back.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TacheService {

    private final TacheRepository tacheRepository;
    private final DossierRepository dossierRepository;
    private final TacheMapper tacheMapper;

    // --- CREATE ---
    public TacheResponse creerTache(Long userId, Long dossierId, TacheCreateRequest request) {
        Dossier dossier = dossierRepository.findByIdAndUserId(dossierId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        Tache tache = Tache.builder()
                .titre(request.titre())
                .description(request.description())
                .dateEcheance(request.dateEcheance())
                .priorite(request.priorite())
                .dossier(dossier)
                .build();

        return tacheMapper.toResponse(tacheRepository.save(tache));
    }

    // --- READ : toutes les tâches d'un dossier ---
    @Transactional(readOnly = true)
    public List<TacheResponse> listerTachesParDossier(Long userId, Long dossierId) {
        // Vérifie l'ownership du dossier avant de lister ses tâches
        if (!dossierRepository.existsByIdAndUserId(dossierId, userId)) {
            throw new ResourceNotFoundException("Dossier introuvable");
        }

        return tacheRepository.findByDossierId(dossierId)
                .stream()
                .map(tacheMapper::toResponse)
                .toList();
    }

    // --- READ : une tâche précise ---
    @Transactional(readOnly = true)
    public TacheResponse obtenirTache(Long userId, Long tacheId) {
        Tache tache = tacheRepository.findByIdAndUserId(tacheId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));

        return tacheMapper.toResponse(tache);
    }

    // --- UPDATE ---
    public TacheResponse modifierTache(Long userId, Long tacheId, TacheUpdateRequest request) {
        Tache tache = tacheRepository.findByIdAndUserId(tacheId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));

        tache.setTitre(request.titre());
        tache.setDescription(request.description());
        tache.setDateEcheance(request.dateEcheance());
        tache.setPriorite(request.priorite());

        return tacheMapper.toResponse(tacheRepository.save(tache));
    }

    // --- UPDATE : changement de statut uniquement ---
    public TacheResponse changerStatut(Long userId, Long tacheId, TacheStatutUpdateRequest request) {
        Tache tache = tacheRepository.findByIdAndUserId(tacheId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));

        tache.setStatut(request.statut());

        return tacheMapper.toResponse(tacheRepository.save(tache));
    }

    // --- DELETE ---
    public void supprimerTache(Long userId, Long tacheId) {
        Tache tache = tacheRepository.findByIdAndUserId(tacheId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));

        tacheRepository.delete(tache);
    }
}