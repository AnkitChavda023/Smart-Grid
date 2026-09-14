package com.smartgrid.vendorservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.vendorservice.dto.VendorRankingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class VendorRankingCacheService {

    private static final Logger log = LoggerFactory.getLogger(VendorRankingCacheService.class);

    private static final String VERSION_KEY = "vendor:cache:version";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public VendorRankingCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<VendorRankingResult>> get(String skuId, int k) {
        String cached = redisTemplate.opsForValue().get(cacheKey(skuId, k));
        if (cached == null) {
            log.info("Vendor ranking cache MISS for sku={} k={}", skuId, k);
            return Optional.empty();
        }
        log.info("Vendor ranking cache HIT for sku={} k={}", skuId, k);
        try {
            return Optional.of(List.of(objectMapper.readValue(cached, VendorRankingResult[].class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void put(String skuId, int k, List<VendorRankingResult> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(cacheKey(skuId, k), json, TTL);
        } catch (Exception e) {
            log.warn("Failed to cache vendor ranking for sku={} k={}", skuId, k, e);
        }
    }

    // Bumping the version orphans every existing cache key instead of scanning/deleting them (no Redis KEYS command).
    public void invalidateAll() {
        redisTemplate.opsForValue().increment(VERSION_KEY);
    }

    private String cacheKey(String skuId, int k) {
        return "vendor:top:v" + currentVersion() + ":" + skuId + ":" + k;
    }

    private long currentVersion() {
        String version = redisTemplate.opsForValue().get(VERSION_KEY);
        return version == null ? 0 : Long.parseLong(version);
    }
}
