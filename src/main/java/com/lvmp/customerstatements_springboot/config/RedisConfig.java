package com.lvmp.customerstatements_springboot.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {
    public static final String PRESIGNED_URLS_CACHE = "presignedUrls";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.lvmp.customerstatements_springboot")
                .build();

        JsonMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(typeValidator, DefaultTyping.NON_FINAL)
                .build();

        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(mapper);

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .withCacheConfiguration(PRESIGNED_URLS_CACHE,
                        cacheConfiguration.entryTtl(Duration.ofMinutes(4).plusSeconds(30)))
                .build();
    }
}