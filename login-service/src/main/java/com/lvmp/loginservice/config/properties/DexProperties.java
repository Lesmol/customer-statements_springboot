package com.lvmp.loginservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dex")
public record DexProperties(
        String tokenUri,
        String clientId,
        String clientSecret) {
}
