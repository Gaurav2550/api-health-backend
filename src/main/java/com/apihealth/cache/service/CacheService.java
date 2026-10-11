package com.apihealth.cache.service;

import com.apihealth.cache.dto.CacheBenchmarkResponse;
import com.apihealth.cache.dto.CacheStatsResponse;
import com.apihealth.cache.memory.CustomInMemoryCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final CustomInMemoryCache<String, Object> inMemoryCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;

    public CacheService(
            @Value("${app.cache.default-max-size:1000}") int maxSize,
            @Value("${app.cache.default-ttl-minutes:5}") int ttlMinutes,
            Optional<RedisTemplate<String, Object>> redisTemplate,
            ObjectMapper objectMapper) {
        this.defaultTtl = Duration.ofMinutes(ttlMinutes);
        this.inMemoryCache = new CustomInMemoryCache<>(maxSize, this.defaultTtl);
        this.redisTemplate = redisTemplate.orElse(null);
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        // 1. Try local in-memory cache first (L1)
        Optional<Object> local = inMemoryCache.get(key);
        if (local.isPresent()) {
            return Optional.of(convertValue(local.get(), clazz));
        }

        // 2. Try Redis distributed cache if available (L2)
        if (redisTemplate != null) {
            try {
                Object redisVal = redisTemplate.opsForValue().get(key);
                if (redisVal != null) {
                    T converted = convertValue(redisVal, clazz);
                    // Populate L1 cache for subsequent fast reads
                    inMemoryCache.put(key, converted, defaultTtl);
                    return Optional.of(converted);
                }
            } catch (Exception e) {
                log.warn("Redis read failed for key '{}': {}. Falling back to in-memory only.", key, e.getMessage());
            }
        }

        return Optional.empty();
    }

    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }

    public void put(String key, Object value, Duration ttl) {
        inMemoryCache.put(key, value, ttl);

        if (redisTemplate != null) {
            try {
                Duration effectiveTtl = (ttl != null && !ttl.isZero()) ? ttl : defaultTtl;
                redisTemplate.opsForValue().set(key, value, effectiveTtl.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("Redis write failed for key '{}': {}", key, e.getMessage());
            }
        }
    }

    public void evict(String key) {
        inMemoryCache.invalidate(key);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Redis evict failed for key '{}': {}", key, e.getMessage());
            }
        }
    }

    public void clear() {
        inMemoryCache.clear();
        if (redisTemplate != null) {
            try {
                var keys = redisTemplate.keys("apihealth:*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                log.warn("Redis clear failed: {}", e.getMessage());
            }
        }
    }

    public CacheStatsResponse getStats() {
        CacheStatsResponse stats = new CacheStatsResponse();
        stats.setInMemorySize(inMemoryCache.size());
        stats.setInMemoryHitCount(inMemoryCache.getHitCount());
        stats.setInMemoryMissCount(inMemoryCache.getMissCount());
        stats.setInMemoryHitRate(inMemoryCache.getHitRate());
        stats.setInMemoryEvictionCount(inMemoryCache.getEvictionCount());

        boolean redisUp = false;
        String redisMsg = "Redis not configured";
        if (redisTemplate != null) {
            try {
                String ping = redisTemplate.getConnectionFactory().getConnection().ping();
                redisUp = "PONG".equalsIgnoreCase(ping);
                redisMsg = redisUp ? "Connected (Healthy)" : "Ping failed: " + ping;
            } catch (Exception e) {
                redisMsg = "Disconnected: " + e.getMessage();
            }
        }
        stats.setRedisConnected(redisUp);
        stats.setRedisStatus(redisMsg);

        return stats;
    }

    public CacheBenchmarkResponse runBenchmark(int iterations) {
        int count = Math.min(Math.max(iterations, 10), 1000);
        String testKey = "benchmark:test:key";
        String payload = "{\"status\":\"UP\",\"latency\":42,\"data\":\"test payload for caching benchmark\"}";

        // 1. Measure direct simulation (mimicking database query / computation)
        long startUncached = System.nanoTime();
        for (int i = 0; i < count; i++) {
            simulateUncachedWork(payload);
        }
        long durationUncached = System.nanoTime() - startUncached;

        // 2. Measure In-Memory Cache (L1)
        inMemoryCache.put(testKey, payload, Duration.ofMinutes(10));
        long startMem = System.nanoTime();
        for (int i = 0; i < count; i++) {
            inMemoryCache.get(testKey);
        }
        long durationMem = System.nanoTime() - startMem;

        // 3. Measure Redis Cache (L2) or simulated remote call
        boolean hasRedis = false;
        long durationRedis = 0;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(testKey, payload, 10, TimeUnit.MINUTES);
                long startRedis = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    redisTemplate.opsForValue().get(testKey);
                }
                durationRedis = System.nanoTime() - startRedis;
                hasRedis = true;
            } catch (Exception e) {
                log.warn("Benchmark Redis read failed: {}", e.getMessage());
            }
        }

        double uncachedAvgMicros = (double) durationUncached / count / 1000.0;
        double memAvgMicros = (double) durationMem / count / 1000.0;
        double redisAvgMicros = hasRedis ? ((double) durationRedis / count / 1000.0) : 0.0;

        double memSpeedup = memAvgMicros > 0 ? (uncachedAvgMicros / memAvgMicros) : 1.0;
        double redisSpeedup = (hasRedis && redisAvgMicros > 0) ? (uncachedAvgMicros / redisAvgMicros) : 1.0;

        CacheBenchmarkResponse res = new CacheBenchmarkResponse();
        res.setIterations(count);
        res.setUncachedAvgMicroseconds(Math.round(uncachedAvgMicros * 100.0) / 100.0);
        res.setInMemoryAvgMicroseconds(Math.round(memAvgMicros * 100.0) / 100.0);
        res.setRedisAvgMicroseconds(Math.round(redisAvgMicros * 100.0) / 100.0);
        res.setInMemorySpeedupMultiplier(Math.round(memSpeedup * 10.0) / 10.0);
        res.setRedisSpeedupMultiplier(Math.round(redisSpeedup * 10.0) / 10.0);
        res.setAnalysis(String.format("In-Memory cache provided %.1fx speedup over uncached query. %s",
                memSpeedup, hasRedis ? String.format("Redis provided %.1fx speedup.", redisSpeedup) : "Redis was unavailable during test."));

        return res;
    }

    private void simulateUncachedWork(String data) {
        // Lightweight hashing simulation to mimic database / JSON parse latency
        int hash = data.hashCode();
        if (hash == 0) log.trace("hash zero");
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object from, Class<T> clazz) {
        if (from == null) return null;
        if (clazz.isInstance(from)) {
            return (T) from;
        }
        return objectMapper.convertValue(from, clazz);
    }
}
