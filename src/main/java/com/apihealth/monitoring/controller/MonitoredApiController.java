package com.apihealth.monitoring.controller;

import com.apihealth.common.response.ApiResponse;
import com.apihealth.metrics.dto.ApiMetricsResponse;
import com.apihealth.metrics.service.MetricsService;
import com.apihealth.monitoring.dto.*;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.service.HealthCheckService;
import com.apihealth.monitoring.service.MonitoredApiService;
import com.apihealth.optimization.dto.RecommendationResponse;
import com.apihealth.optimization.service.OptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/apis", "/api/monitored-apis", "/api/apis"})
@Tag(name = "Monitored APIs", description = "API registration, management, and health check endpoints")
public class MonitoredApiController {

    private final MonitoredApiService apiService;
    private final HealthCheckService healthCheckService;
    private final MetricsService metricsService;
    private final OptimizationService optimizationService;

    public MonitoredApiController(MonitoredApiService apiService, HealthCheckService healthCheckService,
                                  MetricsService metricsService, OptimizationService optimizationService) {
        this.apiService = apiService;
        this.healthCheckService = healthCheckService;
        this.metricsService = metricsService;
        this.optimizationService = optimizationService;
    }

    @PostMapping
    @Operation(summary = "Register a new API for monitoring")
    public ResponseEntity<ApiResponse<MonitoredApiResponse>> create(
            @Valid @RequestBody CreateApiRequest request, Authentication auth) {
        MonitoredApiResponse response = apiService.create(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("API registered", response));
    }

    @GetMapping
    @Operation(summary = "List all monitored APIs for the current user")
    public ResponseEntity<ApiResponse<Page<MonitoredApiResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MonitoredApiResponse> response = apiService.findAll(auth.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a monitored API by ID")
    public ResponseEntity<ApiResponse<MonitoredApiResponse>> getById(
            @PathVariable Long id, Authentication auth) {
        MonitoredApiResponse response = apiService.findById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a monitored API")
    public ResponseEntity<ApiResponse<MonitoredApiResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateApiRequest request, Authentication auth) {
        MonitoredApiResponse response = apiService.update(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("API updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a monitored API")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        apiService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate monitoring for an API")
    public ResponseEntity<ApiResponse<MonitoredApiResponse>> activate(
            @PathVariable Long id, Authentication auth) {
        MonitoredApiResponse response = apiService.activate(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("API activated", response));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate monitoring for an API")
    public ResponseEntity<ApiResponse<MonitoredApiResponse>> deactivate(
            @PathVariable Long id, Authentication auth) {
        MonitoredApiResponse response = apiService.deactivate(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("API deactivated", response));
    }

    @PostMapping("/{id}/check-now")
    @Operation(summary = "Trigger an immediate health check")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> checkNow(
            @PathVariable Long id, Authentication auth) {
        MonitoredApi api = apiService.getOwnedApi(id, auth.getName());
        HealthCheckResponse response = healthCheckService.performManualCheck(api);
        return ResponseEntity.ok(ApiResponse.success("Health check completed", response));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get health check history for an API")
    public ResponseEntity<ApiResponse<Page<HealthCheckResponse>>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        apiService.getOwnedApi(id, auth.getName()); // Ownership check
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<HealthCheckResponse> response = healthCheckService.getHistory(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get metrics for a monitored API")
    public ResponseEntity<ApiResponse<ApiMetricsResponse>> metrics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int windowHours,
            Authentication auth) {
        apiService.getOwnedApi(id, auth.getName()); // Ownership check
        ApiMetricsResponse response = metricsService.getApiMetrics(id, windowHours);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Get optimization recommendations for an API")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> recommendations(
            @PathVariable Long id, Authentication auth) {
        apiService.getOwnedApi(id, auth.getName()); // Ownership check
        List<RecommendationResponse> response = optimizationService.getRecommendations(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
