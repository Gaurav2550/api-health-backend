package com.apihealth.optimization.service;

import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.HealthStatus;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.HealthCheckRepository;
import com.apihealth.monitoring.repository.MonitoredApiRepository;
import com.apihealth.optimization.dto.RecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class OptimizationServiceTest {

    private MonitoredApiRepository apiRepository;
    private HealthCheckRepository healthCheckRepository;
    private OptimizationService optimizationService;

    @BeforeEach
    void setUp() {
        apiRepository = Mockito.mock(MonitoredApiRepository.class);
        healthCheckRepository = Mockito.mock(HealthCheckRepository.class);
        optimizationService = new OptimizationService(apiRepository, healthCheckRepository);
    }

    @Test
    @DisplayName("Should recommend reliability fixes when health checks fail")
    void testFailureRecommendations() {
        MonitoredApi api = new MonitoredApi();
        api.setId(1L);
        api.setName("Test Down API");
        api.setLatencyThresholdMillis(500);

        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        HealthCheck downCheck = new HealthCheck();
        downCheck.setStatus(HealthStatus.DOWN);
        downCheck.setErrorMessage("Connection refused");
        downCheck.setCheckedAt(Instant.now());

        when(healthCheckRepository.findByApiIdSince(eq(1L), any(Instant.class)))
                .thenReturn(List.of(downCheck));

        List<RecommendationResponse> recs = optimizationService.getRecommendations(1L);

        assertFalse(recs.isEmpty());
        assertTrue(recs.stream().anyMatch(r -> "RELIABILITY".equals(r.getCategory())));
    }

    @Test
    @DisplayName("Should recommend caching and indexing when average latency exceeds threshold")
    void testHighLatencyRecommendations() {
        MonitoredApi api = new MonitoredApi();
        api.setId(2L);
        api.setName("Slow API");
        api.setLatencyThresholdMillis(200);

        when(apiRepository.findById(2L)).thenReturn(Optional.of(api));

        HealthCheck slowCheck = new HealthCheck();
        slowCheck.setStatus(HealthStatus.SLOW);
        slowCheck.setResponseTimeMillis(850L);
        slowCheck.setCheckedAt(Instant.now());

        when(healthCheckRepository.findByApiIdSince(eq(2L), any(Instant.class)))
                .thenReturn(List.of(slowCheck));

        List<RecommendationResponse> recs = optimizationService.getRecommendations(2L);

        assertFalse(recs.isEmpty());
        assertTrue(recs.stream().anyMatch(r -> "SERVER".equals(r.getCategory()) && "HIGH".equals(r.getImpact())));
    }
}
