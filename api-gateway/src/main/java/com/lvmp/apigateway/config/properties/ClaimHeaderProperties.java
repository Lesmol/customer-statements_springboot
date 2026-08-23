package com.lvmp.apigateway.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class ClaimHeaderProperties {
    private Map<String, String> claimHeaders = new LinkedHashMap<>();
}