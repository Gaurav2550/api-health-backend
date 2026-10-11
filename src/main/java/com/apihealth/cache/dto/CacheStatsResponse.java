package com.apihealth.cache.dto;

public class CacheStatsResponse {

    private long inMemorySize;
    private long inMemoryHitCount;
    private long inMemoryMissCount;
    private double inMemoryHitRate;
    private long inMemoryEvictionCount;
    private boolean redisConnected;
    private String redisStatus;

    public CacheStatsResponse() {}

    public long getInMemorySize() {
        return inMemorySize;
    }

    public void setInMemorySize(long inMemorySize) {
        this.inMemorySize = inMemorySize;
    }

    public long getInMemoryHitCount() {
        return inMemoryHitCount;
    }

    public void setInMemoryHitCount(long inMemoryHitCount) {
        this.inMemoryHitCount = inMemoryHitCount;
    }

    public long getInMemoryMissCount() {
        return inMemoryMissCount;
    }

    public void setInMemoryMissCount(long inMemoryMissCount) {
        this.inMemoryMissCount = inMemoryMissCount;
    }

    public double getInMemoryHitRate() {
        return inMemoryHitRate;
    }

    public void setInMemoryHitRate(double inMemoryHitRate) {
        this.inMemoryHitRate = inMemoryHitRate;
    }

    public long getInMemoryEvictionCount() {
        return inMemoryEvictionCount;
    }

    public void setInMemoryEvictionCount(long inMemoryEvictionCount) {
        this.inMemoryEvictionCount = inMemoryEvictionCount;
    }

    public boolean isRedisConnected() {
        return redisConnected;
    }

    public void setRedisConnected(boolean redisConnected) {
        this.redisConnected = redisConnected;
    }

    public String getRedisStatus() {
        return redisStatus;
    }

    public void setRedisStatus(String redisStatus) {
        this.redisStatus = redisStatus;
    }
}
