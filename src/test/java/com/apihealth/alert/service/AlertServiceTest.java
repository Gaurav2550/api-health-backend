package com.apihealth.alert.service;

import com.apihealth.alert.entity.Alert;
import com.apihealth.alert.entity.AlertSeverity;
import com.apihealth.alert.entity.AlertStatus;
import com.apihealth.alert.entity.AlertType;
import com.apihealth.alert.repository.AlertRepository;
import com.apihealth.auth.service.AuthService;
import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.HealthStatus;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.HealthCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    private AlertRepository alertRepository;
    private HealthCheckRepository healthCheckRepository;
    private AuthService authService;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertRepository = Mockito.mock(AlertRepository.class);
        healthCheckRepository = Mockito.mock(HealthCheckRepository.class);
        authService = Mockito.mock(AuthService.class);
        alertService = new AlertService(alertRepository, healthCheckRepository, authService);
        ReflectionTestUtils.setField(alertService, "consecutiveFailuresThreshold", 2);
    }

    @Test
    @DisplayName("Should create CRITICAL API_DOWN alert on consecutive failures")
    void testCreateDownAlert() {
        MonitoredApi api = new MonitoredApi();
        api.setId(10L);
        api.setName("Payment Gateway");
        api.setLatencyThresholdMillis(500);

        HealthCheck check1 = new HealthCheck();
        check1.setStatus(HealthStatus.DOWN);
        HealthCheck check2 = new HealthCheck();
        check2.setStatus(HealthStatus.DOWN);

        when(healthCheckRepository.findRecentByApiId(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(check1, check2));
        when(alertRepository.findActiveAlertsByApiAndType(eq(10L), eq(AlertType.API_DOWN), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        alertService.processHealthCheckResult(api, check1);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(captor.capture());

        Alert saved = captor.getValue();
        assertEquals(AlertType.API_DOWN, saved.getAlertType());
        assertEquals(AlertSeverity.CRITICAL, saved.getSeverity());
        assertEquals(AlertStatus.ACTIVE, saved.getStatus());
    }

    @Test
    @DisplayName("Should auto-resolve active alerts when API recovers to UP")
    void testAutoResolveOnRecovery() {
        MonitoredApi api = new MonitoredApi();
        api.setId(10L);
        api.setName("Payment Gateway");

        Alert activeAlert = new Alert();
        activeAlert.setId(99L);
        activeAlert.setStatus(AlertStatus.ACTIVE);
        activeAlert.setAlertType(AlertType.API_DOWN);

        when(alertRepository.findActiveAlertsByApiId(10L, AlertStatus.ACTIVE))
                .thenReturn(List.of(activeAlert));

        HealthCheck upCheck = new HealthCheck();
        upCheck.setStatus(HealthStatus.UP);
        upCheck.setResponseTimeMillis(120L);

        alertService.processHealthCheckResult(api, upCheck);

        assertEquals(AlertStatus.RESOLVED, activeAlert.getStatus());
        assertNotNull(activeAlert.getResolvedAt());
        verify(alertRepository).save(activeAlert);
    }
}
