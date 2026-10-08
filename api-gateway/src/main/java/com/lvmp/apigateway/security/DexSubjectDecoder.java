package com.lvmp.apigateway.security;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
public final class DexSubjectDecoder {

    public static UUID toUserId(String sub) {
        byte[] decoded = Base64.getUrlDecoder().decode(sub);
        int length = decoded[1];
        String userId = new String(decoded, 2, length, StandardCharsets.UTF_8);
        return UUID.fromString(userId);
    }
}