package com.lvmp.loginservice.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DexTokenResponse(
        @JsonProperty("id_token") String idToken,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("token_type") String tokenType
) {
}