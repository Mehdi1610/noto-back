package com.Noto_back.dto;

import com.Noto_back.model.StatutTache;
import jakarta.validation.constraints.NotNull;

public record TacheStatutUpdateRequest(
        @NotNull StatutTache statut
        ) {
}
