package com.apihealth.cache.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomInMemoryCacheTest {

    private CustomInMemoryCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new CustomInMemoryCache<>(3, Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("Should put and retrieve items correctly")
    void testPutAndGet() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        Optional<String> val1 = cache.get("key1");
        Optional<String> val2 = cache.get("key2");
        Optional<String> missing = cache.get("non-existent");

        assertTrue(val1.isPresent());
        assertEquals("value1", val1.get());
        assertTrue(val2.isPresent());
        assertEquals("value2", val2.get());
        assertTrue(missing.isEmpty());

        assertEquals(2, cache.getHitCount());
        assertEquals(1, cache.getMissCount());
        assertEquals(66.67, cache.getHitRate(), 0.1);
    }

    @Test
    @DisplayName("Should evict least-recently-used item when capacity is exceeded")
    void testLruEviction() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        // Access k1 to make k2 the least recently used
        cache.get("k1");

        // Insert 4th item, exceeding capacity of 3
        cache.put("k4", "v4");

        assertEquals(3, cache.size());
        assertTrue(cache.get("k2").isEmpty(), "k2 should have been evicted as LRU");
        assertTrue(cache.get("k1").isPresent());
        assertTrue(cache.get("k3").isPresent());
        assertTrue(cache.get("k4").isPresent());
        assertEquals(1, cache.getEvictionCount());
    }

    @Test
    @DisplayName("Should expire entries after TTL")
    void testTtlExpiration() throws InterruptedException {
        CustomInMemoryCache<String, String> shortTtlCache =
                new CustomInMemoryCache<>(10, Duration.ofMillis(50));

        shortTtlCache.put("fast-key", "fast-value", Duration.ofMillis(50));
        assertTrue(shortTtlCache.get("fast-key").isPresent());

        Thread.sleep(70);

        assertTrue(shortTtlCache.get("fast-key").isEmpty());
    }

    @Test
    @DisplayName("Should invalidate specific keys and clear whole cache")
    void testInvalidateAndClear() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        cache.invalidate("k1");
        assertTrue(cache.get("k1").isEmpty());
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.get("k2").isEmpty());
    }
}
