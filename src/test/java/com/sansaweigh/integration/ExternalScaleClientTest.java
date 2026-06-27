package com.sansaweigh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sansaweigh.domain.ScaleSpecification;
import com.sansaweigh.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalScaleClientTest {

    @Mock private RestTemplate restTemplate;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private ExternalScaleClient client;

    private ScaleSpecification sampleSpec;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "scaleApiUrl", "http://localhost:9090/api/scales");
        ReflectionTestUtils.setField(client, "cacheTtlSeconds", 120L);

        sampleSpec = ScaleSpecification.builder()
                .id("101")
                .name("Test Scale")
                .brand("SansaScale-Pro")
                .maxCapacity(150.0)
                .precision(0.01)
                .lastCalibrationOffset(-0.05)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("Returns cached spec when found in Redis")
    void testGetScaleSpecifications_CacheHit() {
        when(valueOps.get("scale:spec:101")).thenReturn(sampleSpec);
        when(objectMapper.convertValue(sampleSpec, ScaleSpecification.class)).thenReturn(sampleSpec);

        ScaleSpecification result = client.getScaleSpecifications("101");

        assertThat(result).isEqualTo(sampleSpec);
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("Fetches from external API on cache miss and stores in cache")
    void testGetScaleSpecifications_CacheMiss_FetchesFromApi() {
        when(valueOps.get("scale:spec:101")).thenReturn(null);
        when(restTemplate.getForObject("http://localhost:9090/api/scales/101", ScaleSpecification.class))
                .thenReturn(sampleSpec);

        ScaleSpecification result = client.getScaleSpecifications("101");

        assertThat(result).isEqualTo(sampleSpec);
        verify(valueOps).set(eq("scale:spec:101"), eq(sampleSpec), eq(120L), any());
    }

    @Test
    @DisplayName("Fallback: returns cached spec when API unavailable")
    void testFallback_ReturnsCachedSpec() {
        when(valueOps.get("scale:spec:101")).thenReturn(sampleSpec);
        when(objectMapper.convertValue(sampleSpec, ScaleSpecification.class)).thenReturn(sampleSpec);

        ScaleSpecification result = client.fallbackScaleSpec(new RuntimeException("timeout"), "101");

        assertThat(result).isEqualTo(sampleSpec);
    }

    @Test
    @DisplayName("Fallback: returns default spec (id=-1) when no cached spec available")
    void testFallback_ReturnsDefaultSpec() {
        ScaleSpecification defaultSpec = ScaleSpecification.builder().id("-1").build();

        when(valueOps.get("scale:spec:101")).thenReturn(null);
        when(valueOps.get("scale:spec:-1")).thenReturn(defaultSpec);
        when(objectMapper.convertValue(defaultSpec, ScaleSpecification.class)).thenReturn(defaultSpec);

        ScaleSpecification result = client.fallbackScaleSpec(new RuntimeException("timeout"), "101");

        assertThat(result.getId()).isEqualTo("-1");
    }

    @Test
    @DisplayName("Fallback: throws BusinessException when no cache and no default available")
    void testFallback_ThrowsBusinessException() {
        when(valueOps.get("scale:spec:999")).thenReturn(null);
        when(valueOps.get("scale:spec:-1")).thenReturn(null);

        assertThatThrownBy(() -> client.fallbackScaleSpec(new RuntimeException("fail"), "999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    @DisplayName("Does not store null spec in Redis cache")
    void testGetScaleSpecifications_NullResponse_NotCached() {
        when(valueOps.get("scale:spec:404")).thenReturn(null);
        when(restTemplate.getForObject(anyString(), eq(ScaleSpecification.class))).thenReturn(null);

        ScaleSpecification result = client.getScaleSpecifications("404");

        assertThat(result).isNull();
        verify(valueOps, never()).set(anyString(), isNull(), anyLong(), any());
    }
}
