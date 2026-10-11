package com.apihealth.monitoring.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "health_checks")
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitored_api_id", nullable = false)
    private MonitoredApi monitoredApi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthStatus status;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_time_millis")
    private Long responseTimeMillis;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", length = 50)
    private ErrorType errorType;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    public HealthCheck() {}

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) {
            checkedAt = Instant.now();
        }
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MonitoredApi getMonitoredApi() { return monitoredApi; }
    public void setMonitoredApi(MonitoredApi monitoredApi) { this.monitoredApi = monitoredApi; }

    public HealthStatus getStatus() { return status; }
    public void setStatus(HealthStatus status) { this.status = status; }

    public Integer getHttpStatusCode() { return httpStatusCode; }
    public void setHttpStatusCode(Integer httpStatusCode) { this.httpStatusCode = httpStatusCode; }

    public Long getResponseTimeMillis() { return responseTimeMillis; }
    public void setResponseTimeMillis(Long responseTimeMillis) { this.responseTimeMillis = responseTimeMillis; }

    public ErrorType getErrorType() { return errorType; }
    public void setErrorType(ErrorType errorType) { this.errorType = errorType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
}
