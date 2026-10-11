package com.apihealth.monitoring.dto;

import com.apihealth.monitoring.entity.ErrorType;
import com.apihealth.monitoring.entity.HealthStatus;
import java.time.Instant;

public class HealthCheckResponse {

    private Long id;
    private HealthStatus status;
    private Integer httpStatusCode;
    private Long responseTimeMillis;
    private ErrorType errorType;
    private String errorMessage;
    private Instant checkedAt;

    public HealthCheckResponse() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public static HealthCheckResponse fromEntity(com.apihealth.monitoring.entity.HealthCheck entity) {
        HealthCheckResponse r = new HealthCheckResponse();
        r.setId(entity.getId());
        r.setStatus(entity.getStatus());
        r.setHttpStatusCode(entity.getHttpStatusCode());
        r.setResponseTimeMillis(entity.getResponseTimeMillis());
        r.setErrorType(entity.getErrorType());
        r.setErrorMessage(entity.getErrorMessage());
        r.setCheckedAt(entity.getCheckedAt());
        return r;
    }
}
