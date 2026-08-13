package com.Noto_back.dto;

public record DossierMoveRequest(
        Long nouveauParentId //null = redevient un dossier racine
) {
}
