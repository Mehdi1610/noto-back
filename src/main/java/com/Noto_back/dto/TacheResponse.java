package com.Noto_back.dto;

import com.Noto_back.model.Dossier;
import com.Noto_back.model.Priorite;
import com.Noto_back.model.StatutTache;

import java.time.LocalDate;

public record TacheResponse(
        Long id,
        String titre,
        String description,
        StatutTache statut,
        LocalDate dateEcheance,
        Priorite priorite,
        DossierResponse dossier
) {
}
