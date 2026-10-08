package com.lvmp.userservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record ApplicationConfigurationProperties(PaginationProperties pagination) {
    public record PaginationProperties(int maxPageSize, int minPageSize, int minPageNumber) { }
}
