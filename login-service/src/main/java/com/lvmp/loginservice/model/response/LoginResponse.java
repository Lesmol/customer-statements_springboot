package com.lvmp.loginservice.model.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        String idToken,
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public static LoginResponse from(DexTokenResponse token) {
        return LoginResponse.builder()
                .idToken(token.idToken())
                .accessToken(token.accessToken())
                .refreshToken(token.refreshToken())
                .expiresIn(token.expiresIn())
                .tokenType(token.tokenType())
                .build();
    }
}