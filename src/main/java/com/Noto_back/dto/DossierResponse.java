package com.Noto_back.dto;


import java.time.LocalDateTime;

public record DossierResponse(
        Long id,
        String nom,
        String description,
        String couleur,
        Long parentId,
        LocalDateTime createdAt
) {
}
