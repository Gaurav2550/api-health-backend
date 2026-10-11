package com.apihealth.metrics.service;

import com.apihealth.auth.service.AuthService;
import com.apihealth.metrics.dto.*;
import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.HealthStatus;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.HealthCheckRepository;
import com.apihealth.monitoring.repository.MonitoredApiRepository;
import com.apihealth.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metrics calculations:
 * - Availability = (successful checks / total checks) * 100
 *   where successful = UP + SLOW (the API responded, even if slowly)
 * - Error rate = (failed checks / total checks) * 100
 * - P50/P95/P99 are calculated using the nearest-rank method on sorted latency values
 * - Average latency = sum of all latencies / count
 * - Slow check = a check where the API responded but exceeded its latency threshold
 */
@Service
public class MetricsService {

    private final HealthCheckRepository healthCheckRepository;
    private final MonitoredApiRepository monitoredApiRepository;
    private final AuthService authService;

    @Value("${app.metrics.default-window-hours:24}")
    private int defaultWindowHours;

    @Value("${app.metrics.max-window-hours:720}")
    private int maxWindowHours;

    public MetricsService(HealthCheckRepository healthCheckRepository,
                          MonitoredApiRepository monitoredApiRepository,
                          AuthService authService) {
        this.healthCheckRepository = healthCheckRepository;
        this.monitoredApiRepository = monitoredApiRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public ApiMetricsResponse getApiMetrics(Long apiId, int windowHours) {
        windowHours = Math.min(Math.max(windowHours, 1), maxWindowHours);
        Instant since = Instant.now().minus(windowHours, ChronoUnit.HOURS);

        ApiMetricsResponse metrics = new ApiMetricsResponse();
        metrics.setApiId(apiId);
        metrics.setWindowHours(windowHours);

        long totalChecks = healthCheckRepository.countByApiIdSince(apiId, since);
        metrics.setTotalChecks(totalChecks);

        if (totalChecks == 0) {
            return metrics;
        }

        long upChecks = healthCheckRepository.countByApiIdAndStatusSince(apiId, HealthStatus.UP, since);
        long slowChecks = healthCheckRepository.countByApiIdAndStatusSince(apiId, HealthStatus.SLOW, since);
        long downChecks = healthCheckRepository.countByApiIdAndStatusSince(apiId, HealthStatus.DOWN, since);
        long successfulChecks = upChecks + slowChecks;

        metrics.setSuccessfulChecks(successfulChecks);
        metrics.setFailedChecks(downChecks);
        metrics.setSlowCheckCount(slowChecks);
        metrics.setAvailabilityPercentage(round((double) successfulChecks / totalChecks * 100));
        metrics.setErrorRate(round((double) downChecks / totalChecks * 100));

        // Get sorted latencies for percentile calculation
        List<Long> latencies = healthCheckRepository.findLatenciesByApiIdSince(apiId, since);
        if (!latencies.isEmpty()) {
            metrics.setAverageLatencyMs(round(latencies.stream().mapToLong(Long::longValue).average().orElse(0)));
            metrics.setMinLatencyMs(latencies.get(0));
            metrics.setMaxLatencyMs(latencies.get(latencies.size() - 1));
            metrics.setP50LatencyMs(percentile(latencies, 50));
            metrics.setP95LatencyMs(percentile(latencies, 95));
            metrics.setP99LatencyMs(percentile(latencies, 99));
        }

        return metrics;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(String userEmail) {
        User owner = authService.getUserByEmail(userEmail);
        List<MonitoredApi> apis = monitoredApiRepository.findAllByOwnerId(owner.getId());

        DashboardSummary summary = new DashboardSummary();
        summary.setTotalApis(apis.size());
        summary.setActiveApis(apis.stream().filter(MonitoredApi::isActive).count());
        summary.setApisUp(apis.stream().filter(a -> a.getLastStatus() == HealthStatus.UP).count());
        summary.setApisDown(apis.stream().filter(a -> a.getLastStatus() == HealthStatus.DOWN).count());
        summary.setApisSlow(apis.stream().filter(a -> a.getLastStatus() == HealthStatus.SLOW).count());

        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long totalChecks = 0;
        long successfulChecks = 0;
        for (MonitoredApi api : apis) {
            long apiTotal = healthCheckRepository.countByApiIdSince(api.getId(), last24h);
            long apiUp = healthCheckRepository.countByApiIdAndStatusSince(api.getId(), HealthStatus.UP, last24h);
            long apiSlow = healthCheckRepository.countByApiIdAndStatusSince(api.getId(), HealthStatus.SLOW, last24h);
            totalChecks += apiTotal;
            successfulChecks += (apiUp + apiSlow);
        }
        summary.setTotalChecksLast24h(totalChecks);
        summary.setOverallAvailability(totalChecks > 0 ? round((double) successfulChecks / totalChecks * 100) : 100.0);

        return summary;
    }

    @Transactional(readOnly = true)
    public List<LatencyTrendPoint> getLatencyTrends(Long apiId, int windowHours) {
        windowHours = Math.min(Math.max(windowHours, 1), maxWindowHours);
        Instant start = Instant.now().minus(windowHours, ChronoUnit.HOURS);
        Instant end = Instant.now();

        List<HealthCheck> checks = healthCheckRepository.findByApiIdBetween(apiId, start, end);
        if (checks.isEmpty()) {
            return List.of();
        }

        // Group by hour - compute per-hour metrics from raw data, not from pre-aggregated percentiles
        Map<Instant, List<HealthCheck>> grouped = checks.stream()
                .collect(Collectors.groupingBy(hc -> hc.getCheckedAt().truncatedTo(ChronoUnit.HOURS)));

        List<LatencyTrendPoint> trends = new ArrayList<>();
        for (Map.Entry<Instant, List<HealthCheck>> entry : new TreeMap<>(grouped).entrySet()) {
            List<Long> latencies = entry.getValue().stream()
                    .map(HealthCheck::getResponseTimeMillis)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            if (latencies.isEmpty()) continue;

            LatencyTrendPoint point = new LatencyTrendPoint();
            point.setTimestamp(entry.getKey());
            point.setAverageLatencyMs(round(latencies.stream().mapToLong(Long::longValue).average().orElse(0)));
            point.setP95LatencyMs(percentile(latencies, 95));
            point.setCheckCount(entry.getValue().size());
            trends.add(point);
        }

        return trends;
    }

    @Transactional(readOnly = true)
    public List<ApiStatusSummary> getApiStatusSummaries(String userEmail) {
        User owner = authService.getUserByEmail(userEmail);
        List<MonitoredApi> apis = monitoredApiRepository.findAllByOwnerId(owner.getId());
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);

        List<ApiStatusSummary> summaries = new ArrayList<>();
        for (MonitoredApi api : apis) {
            ApiStatusSummary s = new ApiStatusSummary();
            s.setApiId(api.getId());
            s.setApiName(api.getName());
            s.setUrl(api.getUrl());
            s.setLastStatus(api.getLastStatus());
            s.setActive(api.isActive());

            List<Long> latencies = healthCheckRepository.findLatenciesByApiIdSince(api.getId(), last24h);
            s.setAverageLatencyMs(latencies.isEmpty() ? 0
                    : round(latencies.stream().mapToLong(Long::longValue).average().orElse(0)));

            long total = healthCheckRepository.countByApiIdSince(api.getId(), last24h);
            long up = healthCheckRepository.countByApiIdAndStatusSince(api.getId(), HealthStatus.UP, last24h);
            long slow = healthCheckRepository.countByApiIdAndStatusSince(api.getId(), HealthStatus.SLOW, last24h);
            s.setAvailabilityPercentage(total > 0 ? round((double) (up + slow) / total * 100) : 100.0);

            summaries.add(s);
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public List<ApiStatusSummary> getTopSlowestApis(String userEmail, int limit) {
        List<ApiStatusSummary> all = getApiStatusSummaries(userEmail);
        return all.stream()
                .sorted(Comparator.comparingDouble(ApiStatusSummary::getAverageLatencyMs).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Nearest-rank percentile calculation.
     * The input list must be sorted in ascending order.
     * For a percentile P and N data points: index = ceil(P/100 * N) - 1
     */
    public static long percentile(List<Long> sortedLatencies, double percentile) {
        if (sortedLatencies.isEmpty()) return 0;
        if (sortedLatencies.size() == 1) return sortedLatencies.get(0);

        int index = (int) Math.ceil(percentile / 100.0 * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        return sortedLatencies.get(index);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
