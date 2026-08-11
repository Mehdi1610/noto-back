package com.Noto_back.dto;

import com.Noto_back.model.Priorite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TacheUpdateRequest(
        @NotBlank @Size(max = 150) String titre,
        String description,
        LocalDate dateEcheance,
        Priorite priorite
) {
}
