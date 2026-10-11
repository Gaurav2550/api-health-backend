package com.apihealth.cache.dto;

public class CacheBenchmarkResponse {

    private long iterations;
    private double uncachedAvgMicroseconds;
    private double inMemoryAvgMicroseconds;
    private double redisAvgMicroseconds;
    private double inMemorySpeedupMultiplier;
    private double redisSpeedupMultiplier;
    private String analysis;

    public CacheBenchmarkResponse() {}

    public long getIterations() {
        return iterations;
    }

    public void setIterations(long iterations) {
        this.iterations = iterations;
    }

    public double getUncachedAvgMicroseconds() {
        return uncachedAvgMicroseconds;
    }

    public void setUncachedAvgMicroseconds(double uncachedAvgMicroseconds) {
        this.uncachedAvgMicroseconds = uncachedAvgMicroseconds;
    }

    public double getInMemoryAvgMicroseconds() {
        return inMemoryAvgMicroseconds;
    }

    public void setInMemoryAvgMicroseconds(double inMemoryAvgMicroseconds) {
        this.inMemoryAvgMicroseconds = inMemoryAvgMicroseconds;
    }

    public double getRedisAvgMicroseconds() {
        return redisAvgMicroseconds;
    }

    public void setRedisAvgMicroseconds(double redisAvgMicroseconds) {
        this.redisAvgMicroseconds = redisAvgMicroseconds;
    }

    public double getInMemorySpeedupMultiplier() {
        return inMemorySpeedupMultiplier;
    }

    public void setInMemorySpeedupMultiplier(double inMemorySpeedupMultiplier) {
        this.inMemorySpeedupMultiplier = inMemorySpeedupMultiplier;
    }

    public double getRedisSpeedupMultiplier() {
        return redisSpeedupMultiplier;
    }

    public void setRedisSpeedupMultiplier(double redisSpeedupMultiplier) {
        this.redisSpeedupMultiplier = redisSpeedupMultiplier;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
}
