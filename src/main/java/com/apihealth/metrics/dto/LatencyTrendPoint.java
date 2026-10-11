package com.apihealth.metrics.dto;

import java.time.Instant;

public class LatencyTrendPoint {

    private Instant timestamp;
    private double averageLatencyMs;
    private long p95LatencyMs;
    private long checkCount;

    public LatencyTrendPoint() {}

    public LatencyTrendPoint(Instant timestamp, double averageLatencyMs, long p95LatencyMs, long checkCount) {
        this.timestamp = timestamp;
        this.averageLatencyMs = averageLatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.checkCount = checkCount;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public double getAverageLatencyMs() { return averageLatencyMs; }
    public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

    public long getP95LatencyMs() { return p95LatencyMs; }
    public void setP95LatencyMs(long p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }

    public long getCheckCount() { return checkCount; }
    public void setCheckCount(long checkCount) { this.checkCount = checkCount; }
}
