package com.apihealth.metrics.dto;

import com.apihealth.monitoring.entity.HealthStatus;
import java.time.Instant;

public class DashboardSummary {

    private long totalApis;
    private long activeApis;
    private long apisUp;
    private long apisDown;
    private long apisSlow;
    private long totalChecksLast24h;
    private long activeAlerts;
    private double overallAvailability;
    private Instant calculatedAt;

    public DashboardSummary() {
        this.calculatedAt = Instant.now();
    }

    // Getters and setters
    public long getTotalApis() { return totalApis; }
    public void setTotalApis(long totalApis) { this.totalApis = totalApis; }

    public long getActiveApis() { return activeApis; }
    public void setActiveApis(long activeApis) { this.activeApis = activeApis; }

    public long getApisUp() { return apisUp; }
    public void setApisUp(long apisUp) { this.apisUp = apisUp; }

    public long getApisDown() { return apisDown; }
    public void setApisDown(long apisDown) { this.apisDown = apisDown; }

    public long getApisSlow() { return apisSlow; }
    public void setApisSlow(long apisSlow) { this.apisSlow = apisSlow; }

    public long getTotalChecksLast24h() { return totalChecksLast24h; }
    public void setTotalChecksLast24h(long totalChecksLast24h) { this.totalChecksLast24h = totalChecksLast24h; }

    public long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; }

    public double getOverallAvailability() { return overallAvailability; }
    public void setOverallAvailability(double overallAvailability) { this.overallAvailability = overallAvailability; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
