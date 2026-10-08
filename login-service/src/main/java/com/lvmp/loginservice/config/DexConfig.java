package com.lvmp.loginservice.config;

import com.lvmp.loginservice.config.properties.DexProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DexConfig {
    @Bean
    public RestClient dexRestClient(DexProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.tokenUri())
                .build();
    }
}
