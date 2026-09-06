package com.lvmp.customerstatements_springboot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("services.user-service")
public record UserServiceProperties(String uri, Duration connectTimeout, Duration readTimeout) {
}
