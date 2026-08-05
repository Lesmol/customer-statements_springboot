package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.UUID;

import static com.lvmp.customerstatements_springboot.config.RedisConfig.PRESIGNED_URLS_CACHE;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        redisService = new RedisService(cacheManager);
    }

    @Test
    void getPreSignedUrl_fetchCachedResponse() {
        UUID documentId = UUID.randomUUID();
        GetDocumentResponse cachedResponse = GetDocumentResponse.builder()
                .url("https://example.com/presigned")
                .expiresAt(Instant.now())
                .build();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(cache);
        when(cache.get(documentId, GetDocumentResponse.class)).thenReturn(cachedResponse);

        GetDocumentResponse response = redisService.getPreSignedUrl(documentId);

        assertSame(cachedResponse, response);
    }

    @Test
    void getPreSignedUrl_returnsNull_whenNotInCache() {
        UUID documentId = UUID.randomUUID();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(cache);
        when(cache.get(documentId, GetDocumentResponse.class)).thenReturn(null);

        GetDocumentResponse response = redisService.getPreSignedUrl(documentId);

        assertNull(response);
    }

    @Test
    void getPreSignedUrl_returnsNull_whenCacheDoesNotExist() {
        UUID documentId = UUID.randomUUID();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(null);

        GetDocumentResponse response = redisService.getPreSignedUrl(documentId);

        assertNull(response);
    }

    @Test
    void getPreSignedUrl_returnsNull_whenCacheThrowsException() {
        UUID documentId = UUID.randomUUID();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(cache);
        when(cache.get(documentId, GetDocumentResponse.class)).thenThrow(new RuntimeException("Redis unavailable"));

        GetDocumentResponse response = redisService.getPreSignedUrl(documentId);

        assertNull(response);
    }

    @Test
    void putPreSignedUrl_addsResponseToCache() {
        UUID documentId = UUID.randomUUID();
        GetDocumentResponse newResponse = GetDocumentResponse.builder()
                .url("https://example.com/presigned")
                .expiresAt(Instant.now())
                .build();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(cache);

        redisService.putPreSignedUrl(documentId, newResponse);

        verify(cache).put(documentId, newResponse);
    }

    @Test
    void putPreSignedUrl_doesNotThrow_whenCacheDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        GetDocumentResponse newResponse = GetDocumentResponse.builder()
                .url("https://example.com/presigned")
                .expiresAt(Instant.now())
                .build();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(null);

        redisService.putPreSignedUrl(documentId, newResponse);

        verify(cache, never()).put(any(), any());
    }

    @Test
    void putPreSignedUrl_doesNotThrow_whenCachePutFails() {
        UUID documentId = UUID.randomUUID();
        GetDocumentResponse newResponse = GetDocumentResponse.builder()
                .url("https://example.com/presigned")
                .expiresAt(Instant.now())
                .build();

        when(cacheManager.getCache(PRESIGNED_URLS_CACHE)).thenReturn(cache);
        doThrow(new RuntimeException("Redis unavailable")).when(cache).put(documentId, newResponse);

        redisService.putPreSignedUrl(documentId, newResponse);

        verify(cache).put(documentId, newResponse);
    }
}