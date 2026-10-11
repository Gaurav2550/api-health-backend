package com.apihealth.monitoring.entity;

import com.apihealth.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "monitored_apis")
public class MonitoredApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod = HttpMethod.GET;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "check_interval_seconds", nullable = false)
    private int checkIntervalSeconds = 60;

    @Column(name = "timeout_millis", nullable = false)
    private int timeoutMillis = 10000;

    @Column(name = "latency_threshold_millis", nullable = false)
    private int latencyThresholdMillis = 500;

    @Column(name = "expected_status_codes", nullable = false, length = 100)
    private String expectedStatusCodes = "200";

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", length = 20)
    private HealthStatus lastStatus;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public MonitoredApi() {}

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Parses expectedStatusCodes string (comma-separated) and checks if the given code is expected.
     */
    public boolean isExpectedStatusCode(int statusCode) {
        if (expectedStatusCodes == null || expectedStatusCodes.isBlank()) {
            return statusCode >= 200 && statusCode < 300;
        }
        String[] codes = expectedStatusCodes.split(",");
        for (String code : codes) {
            try {
                if (Integer.parseInt(code.trim()) == statusCode) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // skip malformed entries
            }
        }
        return false;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public HttpMethod getHttpMethod() { return httpMethod; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

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
    public Instant getUpdatedAt() { return updatedAt; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
