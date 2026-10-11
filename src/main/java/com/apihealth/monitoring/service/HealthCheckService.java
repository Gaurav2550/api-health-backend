package com.apihealth.monitoring.service;

import com.apihealth.monitoring.client.HealthCheckClient;
import com.apihealth.monitoring.dto.HealthCheckResponse;
import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.HealthCheckRepository;
import com.apihealth.monitoring.repository.MonitoredApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final HealthCheckClient healthCheckClient;
    private final HealthCheckRepository healthCheckRepository;
    private final MonitoredApiRepository monitoredApiRepository;

    public HealthCheckService(HealthCheckClient healthCheckClient,
                              HealthCheckRepository healthCheckRepository,
                              MonitoredApiRepository monitoredApiRepository) {
        this.healthCheckClient = healthCheckClient;
        this.healthCheckRepository = healthCheckRepository;
        this.monitoredApiRepository = monitoredApiRepository;
    }

    /**
     * Executes a health check for the given API and persists the result.
     * Updates the MonitoredApi's last status and check time.
     */
    @Transactional
    public HealthCheck performCheck(MonitoredApi api) {
        try {
            HealthCheck result = healthCheckClient.execute(api);
            result = healthCheckRepository.save(result);

            // Update the monitored API's latest status
            api.setLastStatus(result.getStatus());
            api.setLastCheckedAt(result.getCheckedAt());
            monitoredApiRepository.save(api);

            log.debug("Health check for '{}': status={}, latency={}ms",
                    api.getName(), result.getStatus(), result.getResponseTimeMillis());

            return result;
        } catch (Exception e) {
            log.error("Failed to persist health check for API '{}': {}", api.getName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Performs a manual health check for an owned API.
     */
    @Transactional
    public HealthCheckResponse performManualCheck(MonitoredApi api) {
        HealthCheck result = performCheck(api);
        return HealthCheckResponse.fromEntity(result);
    }

    @Transactional(readOnly = true)
    public Page<HealthCheckResponse> getHistory(Long apiId, Pageable pageable) {
        return healthCheckRepository.findByMonitoredApiIdOrderByCheckedAtDesc(apiId, pageable)
                .map(HealthCheckResponse::fromEntity);
    }
}
