package com.apihealth.monitoring.executor;

import com.apihealth.alert.service.AlertService;
import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.service.HealthCheckService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Bounded executor for running health checks concurrently.
 *
 * <p>Thread pool sizing: defaults to 10 threads, which allows up to 10
 * concurrent health checks. Size according to expected API count and
 * timeout durations. Each thread may block for up to the configured
 * timeout, so pool_size * max_timeout = worst-case total blocking capacity.</p>
 */
@Component
public class HealthCheckExecutor {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckExecutor.class);

    private final ExecutorService executorService;
    private final HealthCheckService healthCheckService;
    private final AlertService alertService;

    // Tracks APIs currently being checked to prevent overlapping checks
    private final Set<Long> inProgressApis = ConcurrentHashMap.newKeySet();

    public HealthCheckExecutor(
            HealthCheckService healthCheckService,
            AlertService alertService,
            @Value("${app.monitoring.thread-pool-size:10}") int threadPoolSize) {
        this.healthCheckService = healthCheckService;
        this.alertService = alertService;

        // Bounded thread pool with bounded queue to prevent unbounded task accumulation
        this.executorService = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Submits a health check for async execution. Prevents overlapping checks
     * for the same API.
     */
    public void submitCheck(MonitoredApi api) {
        Long apiId = api.getId();
        if (!inProgressApis.add(apiId)) {
            log.debug("Skipping check for API '{}' (id={}): check already in progress", api.getName(), apiId);
            return;
        }

        try {
            executorService.submit(() -> {
                try {
                    HealthCheck result = healthCheckService.performCheck(api);
                    // Process alerts based on health check result
                    alertService.processHealthCheckResult(api, result);
                } catch (Exception e) {
                    log.error("Error executing health check for API '{}': {}", api.getName(), e.getMessage());
                } finally {
                    inProgressApis.remove(apiId);
                }
            });
        } catch (RejectedExecutionException e) {
            inProgressApis.remove(apiId);
            log.warn("Health check rejected for API '{}': executor queue full", api.getName());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down health check executor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
