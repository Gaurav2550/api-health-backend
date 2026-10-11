package com.apihealth.monitoring.client;

import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.MonitoredApi;

/**
 * Abstraction for performing HTTP health checks. Enables testability
 * by allowing mock implementations.
 */
public interface HealthCheckClient {

    /**
     * Performs an HTTP request against the target API and returns a
     * populated HealthCheck entity (not yet persisted).
     */
    HealthCheck execute(MonitoredApi api);
}
