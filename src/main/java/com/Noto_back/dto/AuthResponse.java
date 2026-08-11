package com.Noto_back.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
