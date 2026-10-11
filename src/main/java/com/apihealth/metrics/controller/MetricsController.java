package com.apihealth.metrics.controller;

import com.apihealth.common.response.ApiResponse;
import com.apihealth.metrics.dto.ApiMetricsResponse;
import com.apihealth.metrics.dto.ApiStatusSummary;
import com.apihealth.metrics.dto.DashboardSummary;
import com.apihealth.metrics.dto.LatencyTrendPoint;
import com.apihealth.metrics.service.MetricsService;
import com.apihealth.monitoring.service.MonitoredApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics & Observability", description = "Performance metrics, dashboard summaries, and latency trends")
@SecurityRequirement(name = "Bearer Authentication")
public class MetricsController {

    private final MetricsService metricsService;
    private final MonitoredApiService apiService;

    public MetricsController(MetricsService metricsService, MonitoredApiService apiService) {
        this.metricsService = metricsService;
        this.apiService = apiService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get high-level dashboard metrics summary for current user")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard(Authentication auth) {
        DashboardSummary summary = metricsService.getDashboardSummary(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/apis/{id}")
    @Operation(summary = "Get detailed metrics (P50/P95/P99, availability, error rate) for an API")
    public ResponseEntity<ApiResponse<ApiMetricsResponse>> getApiMetrics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int windowHours,
            Authentication auth) {
        apiService.getOwnedApi(id, auth.getName()); // Verify ownership
        ApiMetricsResponse metrics = metricsService.getApiMetrics(id, windowHours);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/apis/{id}/trends")
    @Operation(summary = "Get hourly latency trends for an API")
    public ResponseEntity<ApiResponse<List<LatencyTrendPoint>>> getLatencyTrends(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int windowHours,
            Authentication auth) {
        apiService.getOwnedApi(id, auth.getName()); // Verify ownership
        List<LatencyTrendPoint> trends = metricsService.getLatencyTrends(id, windowHours);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @GetMapping("/apis/summaries")
    @Operation(summary = "Get status summaries for all owned APIs")
    public ResponseEntity<ApiResponse<List<ApiStatusSummary>>> getSummaries(Authentication auth) {
        List<ApiStatusSummary> summaries = metricsService.getApiStatusSummaries(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @GetMapping("/apis/slowest")
    @Operation(summary = "Get top slowest APIs")
    public ResponseEntity<ApiResponse<List<ApiStatusSummary>>> getSlowest(
            @RequestParam(defaultValue = "5") int limit,
            Authentication auth) {
        List<ApiStatusSummary> slowest = metricsService.getTopSlowestApis(auth.getName(), limit);
        return ResponseEntity.ok(ApiResponse.success(slowest));
    }
}
