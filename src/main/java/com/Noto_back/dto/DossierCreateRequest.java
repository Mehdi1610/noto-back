package com.Noto_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DossierCreateRequest(
        @NotBlank @Size(max = 100) String nom,
        @Size(max = 255) String description,
        @Size(max = 20) String couleur,
        Long parentId // null = dossier racine
) {
}
