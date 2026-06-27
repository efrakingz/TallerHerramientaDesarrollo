package com.sansaweigh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sansaweigh.domain.ScaleSpecification;
import com.sansaweigh.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalScaleClient {

    private static final String CACHE_PREFIX = "scale:spec:";
    private static final String DEFAULT_SCALE_KEY = "scale:spec:-1";

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sansaweigh.external.scale-api-url}")
    private String scaleApiUrl;

    @Value("${sansaweigh.redis.scale-cache-ttl}")
    private long cacheTtlSeconds;

    /**
     * Fetches scale specifications from the external API.
     * Results are cached in Redis with a TTL of 120 seconds.
     * Retried up to 3 times with exponential backoff on transient network errors.
     */
    @Retryable(
            retryFor = {RestClientException.class, java.io.IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public ScaleSpecification getScaleSpecifications(String scaleId) {
        String cacheKey = CACHE_PREFIX + scaleId;
        // Check Redis cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Scale spec for {} found in cache", scaleId);
            return objectMapper.convertValue(cached, ScaleSpecification.class);
        }
        // Fetch from external API
        String url = scaleApiUrl + "/" + scaleId;
        log.info("Fetching scale spec from external API: {}", url);
        ScaleSpecification spec = restTemplate.getForObject(url, ScaleSpecification.class);
        if (spec != null) {
            redisTemplate.opsForValue().set(cacheKey, spec, cacheTtlSeconds, TimeUnit.SECONDS);
        }
        return spec;
    }

    /**
     * Fallback method invoked after all retries are exhausted.
     * Attempts to return a cached version first; otherwise loads the default spec (id="-1") from Redis.
     */
    @Recover
    public ScaleSpecification fallbackScaleSpec(Exception ex, String scaleId) {
        log.warn("External API unavailable for scaleId={}. Attempting fallback. Error: {}", scaleId, ex.getMessage());
        String cacheKey = CACHE_PREFIX + scaleId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Returning cached fallback for scaleId={}", scaleId);
            return objectMapper.convertValue(cached, ScaleSpecification.class);
        }
        // Load default spec from Redis
        Object defaultSpec = redisTemplate.opsForValue().get(DEFAULT_SCALE_KEY);
        if (defaultSpec != null) {
            log.info("Returning default spec from Redis (id=-1)");
            return objectMapper.convertValue(defaultSpec, ScaleSpecification.class);
        }
        log.error("No fallback available for scaleId={}", scaleId);
        throw new BusinessException("Scale service unavailable and no fallback found for scaleId=" + scaleId);
    }
}
