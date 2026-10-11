package com.apihealth.alert.dto;

import com.apihealth.alert.entity.Alert;
import com.apihealth.alert.entity.AlertSeverity;
import com.apihealth.alert.entity.AlertStatus;
import com.apihealth.alert.entity.AlertType;

import java.time.Instant;

public class AlertResponse {

    private Long id;
    private Long apiId;
    private String apiName;
    private String apiUrl;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private AlertStatus status;
    private Instant createdAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;

    public AlertResponse() {}

    public static AlertResponse from(Alert alert) {
        AlertResponse response = new AlertResponse();
        response.setId(alert.getId());
        if (alert.getMonitoredApi() != null) {
            response.setApiId(alert.getMonitoredApi().getId());
            response.setApiName(alert.getMonitoredApi().getName());
            response.setApiUrl(alert.getMonitoredApi().getUrl());
        }
        response.setAlertType(alert.getAlertType());
        response.setSeverity(alert.getSeverity());
        response.setMessage(alert.getMessage());
        response.setStatus(alert.getStatus());
        response.setCreatedAt(alert.getCreatedAt());
        response.setAcknowledgedAt(alert.getAcknowledgedAt());
        response.setResolvedAt(alert.getResolvedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApiId() {
        return apiId;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
