package com.apihealth.monitoring.dto;

import com.apihealth.monitoring.entity.HealthStatus;
import com.apihealth.monitoring.entity.HttpMethod;
import java.time.Instant;

public class MonitoredApiResponse {

    private Long id;
    private String name;
    private String url;
    private HttpMethod httpMethod;
    private int checkIntervalSeconds;
    private int timeoutMillis;
    private int latencyThresholdMillis;
    private String expectedStatusCodes;
    private boolean active;
    private HealthStatus lastStatus;
    private Instant lastCheckedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public MonitoredApiResponse() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public HttpMethod getHttpMethod() { return httpMethod; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }

    public int getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(int checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }

    public int getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(int timeoutMillis) { this.timeoutMillis = timeoutMillis; }

    public int getLatencyThresholdMillis() { return latencyThresholdMillis; }
    public void setLatencyThresholdMillis(int latencyThresholdMillis) { this.latencyThresholdMillis = latencyThresholdMillis; }

    public String getExpectedStatusCodes() { return expectedStatusCodes; }
    public void setExpectedStatusCodes(String expectedStatusCodes) { this.expectedStatusCodes = expectedStatusCodes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public HealthStatus getLastStatus() { return lastStatus; }
    public void setLastStatus(HealthStatus lastStatus) { this.lastStatus = lastStatus; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static MonitoredApiResponse fromEntity(com.apihealth.monitoring.entity.MonitoredApi entity) {
        MonitoredApiResponse r = new MonitoredApiResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setUrl(entity.getUrl());
        r.setHttpMethod(entity.getHttpMethod());
        r.setCheckIntervalSeconds(entity.getCheckIntervalSeconds());
        r.setTimeoutMillis(entity.getTimeoutMillis());
        r.setLatencyThresholdMillis(entity.getLatencyThresholdMillis());
        r.setExpectedStatusCodes(entity.getExpectedStatusCodes());
        r.setActive(entity.isActive());
        r.setLastStatus(entity.getLastStatus());
        r.setLastCheckedAt(entity.getLastCheckedAt());
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());
        return r;
    }
}
