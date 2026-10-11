package com.apihealth.alert.service;

import com.apihealth.alert.dto.AlertResponse;
import com.apihealth.alert.entity.Alert;
import com.apihealth.alert.entity.AlertSeverity;
import com.apihealth.alert.entity.AlertStatus;
import com.apihealth.alert.entity.AlertType;
import com.apihealth.alert.repository.AlertRepository;
import com.apihealth.auth.service.AuthService;
import com.apihealth.common.exception.ResourceNotFoundException;
import com.apihealth.monitoring.entity.ErrorType;
import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.HealthStatus;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.HealthCheckRepository;
import com.apihealth.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final HealthCheckRepository healthCheckRepository;
    private final AuthService authService;

    @Value("${app.alerts.consecutive-failures-threshold:3}")
    private int consecutiveFailuresThreshold;

    public AlertService(AlertRepository alertRepository,
                        HealthCheckRepository healthCheckRepository,
                        AuthService authService) {
        this.alertRepository = alertRepository;
        this.healthCheckRepository = healthCheckRepository;
        this.authService = authService;
    }

    /**
     * Evaluates alert rules following a health check result.
     * Generates alerts on threshold breach and auto-resolves when health recovers.
     */
    @Transactional
    public void processHealthCheckResult(MonitoredApi api, HealthCheck check) {
        if (api == null || check == null) return;

        if (check.getStatus() == HealthStatus.DOWN) {
            handleDownStatus(api, check);
        } else if (check.getStatus() == HealthStatus.SLOW) {
            handleSlowStatus(api, check);
        } else if (check.getStatus() == HealthStatus.UP) {
            handleRecovery(api);
        }

        if (check.getErrorType() == ErrorType.TIMEOUT) {
            handleTimeout(api, check);
        }
    }

    private void handleDownStatus(MonitoredApi api, HealthCheck check) {
        // Check consecutive failures
        List<HealthCheck> recentChecks = healthCheckRepository.findRecentByApiId(
                api.getId(), PageRequest.of(0, Math.max(1, consecutiveFailuresThreshold)));

        boolean consecutiveDown = !recentChecks.isEmpty() &&
                recentChecks.stream().allMatch(c -> c.getStatus() == HealthStatus.DOWN);

        if (consecutiveDown) {
            List<Alert> existing = alertRepository.findActiveAlertsByApiAndType(
                    api.getId(), AlertType.API_DOWN, AlertStatus.ACTIVE);

            if (existing.isEmpty()) {
                Alert alert = new Alert();
                alert.setMonitoredApi(api);
                alert.setAlertType(AlertType.API_DOWN);
                alert.setSeverity(AlertSeverity.CRITICAL);
                String detail = check.getErrorMessage() != null ? check.getErrorMessage()
                        : (check.getHttpStatusCode() != null ? "HTTP " + check.getHttpStatusCode() : "Unreachable");
                alert.setMessage(String.format("API '%s' is DOWN. Consecutive failure threshold reached. Detail: %s",
                        api.getName(), detail));
                alert.setStatus(AlertStatus.ACTIVE);
                alertRepository.save(alert);
                log.warn("Created CRITICAL API_DOWN alert for API '{}' (id={})", api.getName(), api.getId());
            }
        }
    }

    private void handleSlowStatus(MonitoredApi api, HealthCheck check) {
        List<Alert> existing = alertRepository.findActiveAlertsByApiAndType(
                api.getId(), AlertType.HIGH_LATENCY, AlertStatus.ACTIVE);

        if (existing.isEmpty()) {
            Alert alert = new Alert();
            alert.setMonitoredApi(api);
            alert.setAlertType(AlertType.HIGH_LATENCY);
            long latency = check.getResponseTimeMillis() != null ? check.getResponseTimeMillis() : 0;
            alert.setSeverity(latency > (long) api.getLatencyThresholdMillis() * 2 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM);
            alert.setMessage(String.format("API '%s' latency of %dms exceeded target SLA of %dms",
                    api.getName(), latency, api.getLatencyThresholdMillis()));
            alert.setStatus(AlertStatus.ACTIVE);
            alertRepository.save(alert);
            log.info("Created HIGH_LATENCY alert for API '{}' (id={})", api.getName(), api.getId());
        }
    }

    private void handleTimeout(MonitoredApi api, HealthCheck check) {
        List<Alert> existing = alertRepository.findActiveAlertsByApiAndType(
                api.getId(), AlertType.REPEATED_TIMEOUT, AlertStatus.ACTIVE);

        if (existing.isEmpty()) {
            Alert alert = new Alert();
            alert.setMonitoredApi(api);
            alert.setAlertType(AlertType.REPEATED_TIMEOUT);
            alert.setSeverity(AlertSeverity.HIGH);
            alert.setMessage(String.format("API '%s' request timed out after configured limit of %dms",
                    api.getName(), api.getTimeoutMillis()));
            alert.setStatus(AlertStatus.ACTIVE);
            alertRepository.save(alert);
            log.info("Created REPEATED_TIMEOUT alert for API '{}' (id={})", api.getName(), api.getId());
        }
    }

    private void handleRecovery(MonitoredApi api) {
        List<Alert> activeAlerts = alertRepository.findActiveAlertsByApiId(api.getId(), AlertStatus.ACTIVE);
        if (!activeAlerts.isEmpty()) {
            Instant now = Instant.now();
            for (Alert alert : activeAlerts) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(now);
                alertRepository.save(alert);
                log.info("Auto-resolved alert {} ({}) for recovered API '{}'",
                        alert.getId(), alert.getAlertType(), api.getName());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> getAlerts(String userEmail, AlertStatus status, AlertSeverity severity, Pageable pageable) {
        User user = authService.getUserByEmail(userEmail);
        Page<Alert> alertPage = alertRepository.findByOwnerId(user.getId(), pageable);

        List<AlertResponse> filtered = alertPage.getContent().stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> severity == null || a.getSeverity() == severity)
                .map(AlertResponse::from)
                .toList();

        return new PageImpl<>(filtered, pageable, alertPage.getTotalElements());
    }

    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId, String userEmail) {
        User user = authService.getUserByEmail(userEmail);
        Alert alert = alertRepository.findByIdAndOwnerId(alertId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        if (alert.getStatus() == AlertStatus.ACTIVE) {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(Instant.now());
            alert = alertRepository.save(alert);
        }
        return AlertResponse.from(alert);
    }

    @Transactional
    public AlertResponse resolveAlert(Long alertId, String userEmail) {
        User user = authService.getUserByEmail(userEmail);
        Alert alert = alertRepository.findByIdAndOwnerId(alertId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        if (alert.getStatus() != AlertStatus.RESOLVED) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(Instant.now());
            alert = alertRepository.save(alert);
        }
        return AlertResponse.from(alert);
    }
}
