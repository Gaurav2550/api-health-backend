package com.apihealth.optimization.service;

import com.apihealth.common.exception.ResourceNotFoundException;
import com.apihealth.monitoring.entity.ErrorType;
import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.HealthStatus;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.HealthCheckRepository;
import com.apihealth.monitoring.repository.MonitoredApiRepository;
import com.apihealth.optimization.dto.RecommendationCategory;
import com.apihealth.optimization.dto.RecommendationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OptimizationService {

    private final MonitoredApiRepository apiRepository;
    private final HealthCheckRepository healthCheckRepository;

    public OptimizationService(MonitoredApiRepository apiRepository,
                               HealthCheckRepository healthCheckRepository) {
        this.apiRepository = apiRepository;
        this.healthCheckRepository = healthCheckRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long apiId) {
        MonitoredApi api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitored API not found with id: " + apiId));

        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<HealthCheck> checks = healthCheckRepository.findByApiIdSince(apiId, since);

        if (checks.isEmpty()) {
            // Check recent history without 24h limit if fresh
            checks = healthCheckRepository.findRecentByApiId(apiId, PageRequest.of(0, 50));
        }

        List<RecommendationResponse> recommendations = new ArrayList<>();

        if (checks.isEmpty()) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.ARCHITECTURE.name(),
                    "Insufficient Telemetry Data",
                    "There are not enough health check samples yet to generate optimization recommendations. Wait for automated checks to run.",
                    "LOW",
                    List.of(
                            "Trigger a manual health check or allow scheduled checks to run",
                            "Verify that the target API endpoint is actively monitored"
                    )
            ));
            return recommendations;
        }

        long totalChecks = checks.size();
        long downCount = checks.stream().filter(c -> c.getStatus() == HealthStatus.DOWN).count();
        long slowCount = checks.stream().filter(c -> c.getStatus() == HealthStatus.SLOW).count();
        long timeoutCount = checks.stream().filter(c -> c.getErrorType() == ErrorType.TIMEOUT).count();

        List<Long> latencies = checks.stream()
                .map(HealthCheck::getResponseTimeMillis)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        double avgLatency = latencies.isEmpty() ? 0 : latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95Latency = latencies.isEmpty() ? 0 : latencies.get(Math.min((int) Math.ceil(0.95 * latencies.size()) - 1, latencies.size() - 1));
        long p50Latency = latencies.isEmpty() ? 0 : latencies.get(Math.min((int) Math.ceil(0.50 * latencies.size()) - 1, latencies.size() - 1));

        // Rule 1: High Error Rate / Outages
        double errorRate = (double) downCount / totalChecks * 100.0;
        if (downCount > 0 || errorRate > 5.0) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.RELIABILITY.name(),
                    "Elevated Error Rate & Availability Degradation",
                    String.format("Observed %d failed checks (%.1f%% error rate) in recent observation window.", downCount, errorRate),
                    "HIGH",
                    List.of(
                            "Implement Circuit Breaker (e.g. Resilience4j) to prevent cascading upstream failures",
                            "Configure exponential backoff and jitter retries for transient HTTP failures",
                            "Review target server error logs and database connection pool saturation",
                            "Set up automated alert notifications for team on-call escalation"
                    )
            ));
        }

        // Rule 2: High Latency exceeding configured threshold
        if (avgLatency > api.getLatencyThresholdMillis()) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.SERVER.name(),
                    "High Average Latency Exceeding SLA Threshold",
                    String.format("Average response time of %.0fms exceeds your target threshold of %dms.", avgLatency, api.getLatencyThresholdMillis()),
                    "HIGH",
                    List.of(
                            "Implement Redis or in-memory caching for frequently requested dynamic endpoints",
                            "Analyze and index slow database queries used by this endpoint",
                            "Profile backend execution bottlenecks and avoid synchronous remote dependencies",
                            "Consider read replicas for read-heavy database query patterns"
                    )
            ));
        }

        // Rule 3: Timeouts observed
        if (timeoutCount > 0) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.NETWORK.name(),
                    "Frequent HTTP Connection / Read Timeouts",
                    String.format("%d requests timed out against timeout threshold of %dms.", timeoutCount, api.getTimeoutMillis()),
                    "HIGH",
                    List.of(
                            "Verify network path, firewalls, and proxy keep-alive settings",
                            "Ensure server thread pools and worker queues are not starved under concurrent load",
                            "Increase client read timeout or implement asynchronous request queuing"
                    )
            ));
        }

        // Rule 4: High Tail Latency (P95 vs P50)
        if (p95Latency > 2 * p50Latency && p95Latency > 200) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.ARCHITECTURE.name(),
                    "Severe Tail Latency Degradation (P95 vs P50)",
                    String.format("P95 latency (%dms) is significantly worse than P50 median (%dms), indicating periodic latency spikes.", p95Latency, p50Latency),
                    "MEDIUM",
                    List.of(
                            "Implement token bucket rate limiting to prevent traffic burst contention",
                            "Investigate JVM garbage collection pauses (e.g. switch to G1GC or ZGC)",
                            "Review lock contention and synchronized blocks in backend controllers"
                    )
            ));
        }

        // Rule 5: Frequent Slow Responses
        double slowRate = (double) slowCount / totalChecks * 100.0;
        if (slowRate > 15.0 && avgLatency <= api.getLatencyThresholdMillis()) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.PAYLOAD.name(),
                    "Intermittent Slow Response Bursts",
                    String.format("%.1f%% of requests were flagged as SLOW.", slowRate),
                    "MEDIUM",
                    List.of(
                            "Enable Gzip/Brotli payload compression to reduce transfer overhead",
                            "Paginate large responses and limit maximum items per page",
                            "Filter JSON fields to exclude unused relational data (sparse fieldsets)"
                    )
            ));
        }

        // General Best Practice / Baseline
        if (recommendations.isEmpty()) {
            recommendations.add(new RecommendationResponse(
                    RecommendationCategory.ARCHITECTURE.name(),
                    "Healthy API Baseline Performance",
                    String.format("API '%s' is operating within healthy parameters (Avg: %.0fms, Availability: %.1f%%).", api.getName(), avgLatency, 100.0 - errorRate),
                    "LOW",
                    List.of(
                            "Maintain continuous automated polling to detect transient regressions early",
                            "Ensure TLS 1.3 session resumption and HTTP Keep-Alive are enabled for client connections",
                            "Review latency percentiles periodically during high-traffic peak windows"
                    )
            ));
        }

        return recommendations;
    }
}
