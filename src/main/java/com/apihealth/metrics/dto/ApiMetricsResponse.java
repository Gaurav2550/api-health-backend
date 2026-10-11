package com.apihealth.metrics.dto;

import java.time.Instant;
import java.util.List;

public class ApiMetricsResponse {

    private Long apiId;
    private long totalChecks;
    private long successfulChecks;
    private long failedChecks;
    private double availabilityPercentage;
    private double errorRate;
    private double averageLatencyMs;
    private long minLatencyMs;
    private long maxLatencyMs;
    private long p50LatencyMs;
    private long p95LatencyMs;
    private long p99LatencyMs;
    private long slowCheckCount;
    private int windowHours;
    private Instant calculatedAt;

    public ApiMetricsResponse() {
        this.calculatedAt = Instant.now();
    }

    // Getters and setters
    public Long getApiId() { return apiId; }
    public void setApiId(Long apiId) { this.apiId = apiId; }

    public long getTotalChecks() { return totalChecks; }
    public void setTotalChecks(long totalChecks) { this.totalChecks = totalChecks; }

    public long getSuccessfulChecks() { return successfulChecks; }
    public void setSuccessfulChecks(long successfulChecks) { this.successfulChecks = successfulChecks; }

    public long getFailedChecks() { return failedChecks; }
    public void setFailedChecks(long failedChecks) { this.failedChecks = failedChecks; }

    public double getAvailabilityPercentage() { return availabilityPercentage; }
    public void setAvailabilityPercentage(double availabilityPercentage) { this.availabilityPercentage = availabilityPercentage; }

    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

    public double getAverageLatencyMs() { return averageLatencyMs; }
    public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

    public long getMinLatencyMs() { return minLatencyMs; }
    public void setMinLatencyMs(long minLatencyMs) { this.minLatencyMs = minLatencyMs; }

    public long getMaxLatencyMs() { return maxLatencyMs; }
    public void setMaxLatencyMs(long maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }

    public long getP50LatencyMs() { return p50LatencyMs; }
    public void setP50LatencyMs(long p50LatencyMs) { this.p50LatencyMs = p50LatencyMs; }

    public long getP95LatencyMs() { return p95LatencyMs; }
    public void setP95LatencyMs(long p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }

    public long getP99LatencyMs() { return p99LatencyMs; }
    public void setP99LatencyMs(long p99LatencyMs) { this.p99LatencyMs = p99LatencyMs; }

    public long getSlowCheckCount() { return slowCheckCount; }
    public void setSlowCheckCount(long slowCheckCount) { this.slowCheckCount = slowCheckCount; }

    public int getWindowHours() { return windowHours; }
    public void setWindowHours(int windowHours) { this.windowHours = windowHours; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
