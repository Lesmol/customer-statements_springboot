package com.lvmp.customerstatements_springboot.service;

import com.lvmp.customerstatements_springboot.model.response.GetDocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.lvmp.customerstatements_springboot.config.RedisConfig.PRESIGNED_URLS_CACHE;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {
    private final CacheManager cacheManager;

    public GetDocumentResponse getPreSignedUrl(UUID documentId) {
        try {
            Cache presignedUrlCache = cacheManager.getCache(PRESIGNED_URLS_CACHE);
            GetDocumentResponse response = presignedUrlCache.get(documentId, GetDocumentResponse.class);

            if (response != null) {
                log.info("{} was retrieved from Redis", documentId);
                return response;
            }

            log.info("{} does not exist in Redis", documentId);
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public void putPreSignedUrl(UUID documentId, GetDocumentResponse response) {
        try {
            Cache presignedUrlCache = cacheManager.getCache(PRESIGNED_URLS_CACHE);
            presignedUrlCache.put(documentId, response);
            log.info("{} was added to Redis", documentId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
