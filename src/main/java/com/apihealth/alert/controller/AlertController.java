package com.apihealth.alert.controller;

import com.apihealth.alert.dto.AlertResponse;
import com.apihealth.alert.entity.AlertSeverity;
import com.apihealth.alert.entity.AlertStatus;
import com.apihealth.alert.service.AlertService;
import com.apihealth.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert management, acknowledgment, and resolution")
@SecurityRequirement(name = "Bearer Authentication")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Get user alerts with optional status and severity filtering")
    public ResponseEntity<ApiResponse<Page<AlertResponse>>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        Page<AlertResponse> alerts = alertService.getAlerts(auth.getName(), status, severity, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an active alert")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledge(
            @PathVariable Long id,
            Authentication auth) {
        AlertResponse response = alertService.acknowledgeAlert(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Alert acknowledged successfully", response));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Manually resolve an alert")
    public ResponseEntity<ApiResponse<AlertResponse>> resolve(
            @PathVariable Long id,
            Authentication auth) {
        AlertResponse response = alertService.resolveAlert(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Alert resolved successfully", response));
    }
}
