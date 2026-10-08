package com.lvmp.customerstatements_springboot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record ApplicationConfigurationProperties(RedisProperties redis, S3Properties s3,
                                                 PaginationProperties pagination) {

    public record RedisProperties(long presignUrlTtlSeconds) { }
    public record S3Properties(String bucketName, long presignUrlExpirationSeconds) { }
    public record PaginationProperties(int maxPageSize, int minPageSize, int minPageNumber) { }
}