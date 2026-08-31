package com.Noto_back.dto;

import java.util.List;

public record DossierTreeResponse(
        Long id,
        String nom,
        String description,
        String couleur,
        Long parentId,
        String parentNom,
        List<DossierTreeResponse> sousDossiers,
        List<TacheResponse> taches
) {
}
