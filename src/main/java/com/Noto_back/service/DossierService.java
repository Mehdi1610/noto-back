package com.Noto_back.service;

import com.Noto_back.dto.*;
import com.Noto_back.exceptions.CycleDetectedException;
import com.Noto_back.exceptions.ResourceNotFoundException;
import com.Noto_back.mapper.DossierMapper;
import com.Noto_back.model.Dossier;
import com.Noto_back.model.User;
import com.Noto_back.repository.DossierRepository;
import com.Noto_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DossierService {

    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;
    private final DossierMapper dossierMapper;

    // --- CREATE ---
    public DossierResponse creerDossier(Long userId, DossierCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Dossier parent = null;
        if (request.parentId() != null) {
            parent = dossierRepository.findByIdAndUserId(request.parentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dossier parent introuvable"));
        }

        Dossier dossier = Dossier.builder()
                .nom(request.nom())
                .description(request.description())
                .couleur(request.couleur())
                .user(user)
                .parent(parent)
                .build();

        return dossierMapper.toResponse(dossierRepository.save(dossier));
    }

    // --- READ : dossiers racines ---
    @Transactional(readOnly = true)
    public List<DossierResponse> listerDossiersRacines(Long userId) {
        return dossierRepository.findByUserIdAndParentIsNull(userId)
                .stream()
                .map(dossierMapper::toResponse)
                .toList();
    }

    // --- READ : arborescence complète ---
    @Transactional(readOnly = true)
    public List<DossierTreeResponse>obtenirArborescence(Long userId) {
        List<Dossier> tousLesDossiers = dossierRepository.findByUserId(userId);

        // Regroupe les dossiers par parentId (null = racine)
        Map<Long, List<Dossier>> parEnfantsDeParent = tousLesDossiers.stream()
                .filter(d -> d.getParent() != null)
                .collect(Collectors.groupingBy(d -> d.getParent().getId()));

        List<Dossier> racines = tousLesDossiers.stream()
                .filter(d -> d.getParent() == null)
                .toList();

        return racines.stream()
                .map(racine -> construireArbre(racine, parEnfantsDeParent))
                .toList();
    }

    private DossierTreeResponse construireArbre(Dossier dossier, Map<Long, List<Dossier>> parEnfantsDeParent) {
        List<Dossier> enfants = parEnfantsDeParent.getOrDefault(dossier.getId(), List.of());

        List<DossierTreeResponse> sousDossiers = enfants.stream()
                .map(enfant -> construireArbre(enfant, parEnfantsDeParent))
                .toList();

        List<TacheResponse> taches = dossier.getTaches().stream()
                .map(t -> new TacheResponse(
                        t.getId(), t.getTitre(), t.getDescription(),
                        t.getStatut(), t.getDateEcheance(), t.getPriorite(), dossierMapper.toResponse(t.getDossier())
                ))
                .toList();

        return new DossierTreeResponse(
                dossier.getId(), dossier.getNom(), dossier.getDescription(),
                dossier.getCouleur(),
                dossier.getParent() != null ? dossier.getParent().getId() : null,
                dossier.getParent() != null ? dossier.getParent().getNom() : null,
                sousDossiers, taches
        );
    }

    // --- READ : détail d'un dossier (sous-dossiers directs + tâches) ---
    @Transactional(readOnly = true)
    public DossierTreeResponse obtenirDossier(Long userId, Long dossierId) {
        Dossier dossier = dossierRepository.findByIdAndUserIdWithTaches(dossierId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        List<Dossier> sousDossiersDirects = dossierRepository.findByParentId(dossierId);

        List<DossierTreeResponse> sousDossiers = sousDossiersDirects.stream()
                .map(sd -> new DossierTreeResponse(sd.getId(), sd.getNom(), sd.getDescription(),
                        sd.getCouleur(), dossierId, dossier.getNom(), List.of(), List.of())) // pas de récursivité profonde ici, juste le niveau direct
                .toList();

        List<TacheResponse> taches = dossier.getTaches().stream()
                .map(t -> new TacheResponse(
                        t.getId(), t.getTitre(), t.getDescription(),
                        t.getStatut(), t.getDateEcheance(), t.getPriorite(), dossierMapper.toResponse(t.getDossier())
                ))
                .toList();

        return new DossierTreeResponse(dossier.getId(), dossier.getNom(), dossier.getDescription(),
                dossier.getCouleur(),
                dossier.getParent() != null ? dossier.getParent().getId() : null,
                dossier.getParent() != null ? dossier.getParent().getNom() : null,
                sousDossiers, taches);
    }

    // --- UPDATE ---
    public DossierResponse modifierDossier(Long userId, Long dossierId, DossierUpdateRequest request) {
        Dossier dossier = dossierRepository.findByIdAndUserId(dossierId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        dossier.setNom(request.nom());
        dossier.setDescription(request.description());
        dossier.setCouleur(request.couleur());
        dossierRepository.save(dossier);
        return dossierMapper.toResponse(dossier);
    }

    // --- MOVE (déplacer dans l'arborescence) ---
    public DossierResponse deplacerDossier(Long userId, Long dossierId, Long nouveauParentId) {
        Dossier dossier = dossierRepository.findByIdAndUserId(dossierId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (nouveauParentId == null) {
            dossier.setParent(null); // devient un dossier racine
            return dossierMapper.toResponse(dossier);
        }

        if (nouveauParentId.equals(dossierId)) {
            throw new CycleDetectedException("Un dossier ne peut pas être son propre parent");
        }

        Dossier nouveauParent = dossierRepository.findByIdAndUserId(nouveauParentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier parent introuvable"));

        if (estDescendant(nouveauParent, dossierId)) {
            throw new CycleDetectedException("Impossible de déplacer un dossier dans l'un de ses propres sous-dossiers");
        }

        dossier.setParent(nouveauParent);
        return dossierMapper.toResponse(dossier);
    }

    // Vérifie si "candidat" est un descendant du dossier "dossierId" en remontant la chaîne des parents
    private boolean estDescendant(Dossier candidat, Long dossierId) {
        Dossier courant = candidat;
        while (courant.getParent() != null) {
            if (courant.getParent().getId().equals(dossierId)) {
                return true;
            }
            courant = courant.getParent();
        }
        return false;
    }

    // --- DELETE ---
    public void supprimerDossier(Long userId, Long dossierId) {
        Dossier dossier = dossierRepository.findByIdAndUserId(dossierId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        dossierRepository.delete(dossier); // cascade + orphanRemoval s'occupent des sous-dossiers et tâches
    }
}