package com.lvmp.apigateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "gateway")
public record ClaimHeaderProperties(Map<String, String> claimHeaders) { }
