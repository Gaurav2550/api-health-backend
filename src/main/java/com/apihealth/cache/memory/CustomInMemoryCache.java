package com.apihealth.cache.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe generic in-memory cache with:
 * - TTL (Time-To-Live) expiration
 * - LRU eviction when max capacity is reached
 * - Atomic metrics collection (hits, misses, evictions, hit-rate)
 */
public class CustomInMemoryCache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(CustomInMemoryCache.class);

    private final int maxCapacity;
    private final Duration defaultTtl;
    private final Map<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
    private final LinkedHashSet<K> accessOrder = new LinkedHashSet<>();
    private final ReentrantLock evictLock = new ReentrantLock();

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    public CustomInMemoryCache(int maxCapacity, Duration defaultTtl) {
        this.maxCapacity = Math.max(1, maxCapacity);
        this.defaultTtl = defaultTtl;
    }

    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    public void put(K key, V value, Duration ttl) {
        if (key == null || value == null) return;

        Instant expiresAt = (ttl != null && !ttl.isZero()) ? Instant.now().plus(ttl) : Instant.MAX;
        CacheEntry<V> entry = new CacheEntry<>(value, expiresAt);

        evictLock.lock();
        try {
            if (!map.containsKey(key) && map.size() >= maxCapacity) {
                evictExpiredOrLru();
            }
            map.put(key, entry);
            accessOrder.remove(key);
            accessOrder.add(key);
        } finally {
            evictLock.unlock();
        }
    }

    public Optional<V> get(K key) {
        if (key == null) return Optional.empty();

        CacheEntry<V> entry = map.get(key);
        if (entry == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        if (entry.isExpired()) {
            evictLock.lock();
            try {
                map.remove(key);
                accessOrder.remove(key);
                evictionCount.incrementAndGet();
            } finally {
                evictLock.unlock();
            }
            missCount.incrementAndGet();
            return Optional.empty();
        }

        // Record hit and record access
        hitCount.incrementAndGet();
        evictLock.lock();
        try {
            accessOrder.remove(key);
            accessOrder.add(key);
        } finally {
            evictLock.unlock();
        }

        return Optional.of(entry.getValue());
    }

    public void invalidate(K key) {
        if (key == null) return;
        evictLock.lock();
        try {
            map.remove(key);
            accessOrder.remove(key);
        } finally {
            evictLock.unlock();
        }
    }

    public void clear() {
        evictLock.lock();
        try {
            map.clear();
            accessOrder.clear();
        } finally {
            evictLock.unlock();
        }
    }

    public int size() {
        return map.size();
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public long getEvictionCount() {
        return evictionCount.get();
    }

    public double getHitRate() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        if (total == 0) return 0.0;
        return Math.round(((double) hits / total * 100.0) * 100.0) / 100.0;
    }

    private void evictExpiredOrLru() {
        // First try to find and evict an expired entry
        Iterator<Map.Entry<K, CacheEntry<V>>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, CacheEntry<V>> entry = it.next();
            if (entry.getValue().isExpired()) {
                accessOrder.remove(entry.getKey());
                it.remove();
                evictionCount.incrementAndGet();
                return;
            }
        }

        // Otherwise evict LRU (first element in accessOrder)
        Iterator<K> lruIt = accessOrder.iterator();
        if (lruIt.hasNext()) {
            K oldestKey = lruIt.next();
            lruIt.remove();
            map.remove(oldestKey);
            evictionCount.incrementAndGet();
            log.debug("LRU evicted key: {}", oldestKey);
        }
    }

    private static class CacheEntry<V> {
        private final V value;
        private final Instant expiresAt;

        CacheEntry(V value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        V getValue() {
            return value;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
