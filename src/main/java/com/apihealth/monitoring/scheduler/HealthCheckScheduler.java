package com.apihealth.monitoring.scheduler;

import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.executor.HealthCheckExecutor;
import com.apihealth.monitoring.repository.MonitoredApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler that dispatches health checks for active APIs based on their
 * configured intervals. Runs every 10 seconds and checks which APIs are
 * due for their next health check.
 */
@Component
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final MonitoredApiRepository apiRepository;
    private final HealthCheckExecutor executor;

    public HealthCheckScheduler(MonitoredApiRepository apiRepository, HealthCheckExecutor executor) {
        this.apiRepository = apiRepository;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${app.monitoring.scheduler-poll-interval-ms:10000}")
    public void scheduleHealthChecks() {
        List<MonitoredApi> activeApis = apiRepository.findByActiveTrue();
        Instant now = Instant.now();

        for (MonitoredApi api : activeApis) {
            if (isDueForCheck(api, now)) {
                executor.submitCheck(api);
            }
        }
    }

    private boolean isDueForCheck(MonitoredApi api, Instant now) {
        if (api.getLastCheckedAt() == null) {
            return true; // Never checked before
        }
        long secondsSinceLastCheck = now.getEpochSecond() - api.getLastCheckedAt().getEpochSecond();
        return secondsSinceLastCheck >= api.getCheckIntervalSeconds();
    }
}
