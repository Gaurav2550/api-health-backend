package com.apihealth.metrics.dto;

import com.apihealth.monitoring.entity.HealthStatus;

public class ApiStatusSummary {

    private Long apiId;
    private String apiName;
    private String url;
    private HealthStatus lastStatus;
    private boolean active;
    private double averageLatencyMs;
    private double availabilityPercentage;

    public ApiStatusSummary() {}

    public Long getApiId() { return apiId; }
    public void setApiId(Long apiId) { this.apiId = apiId; }

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public HealthStatus getLastStatus() { return lastStatus; }
    public void setLastStatus(HealthStatus lastStatus) { this.lastStatus = lastStatus; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getAverageLatencyMs() { return averageLatencyMs; }
    public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

    public double getAvailabilityPercentage() { return availabilityPercentage; }
    public void setAvailabilityPercentage(double availabilityPercentage) { this.availabilityPercentage = availabilityPercentage; }
}
