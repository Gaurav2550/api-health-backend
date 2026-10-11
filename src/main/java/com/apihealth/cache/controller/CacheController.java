package com.apihealth.cache.controller;

import com.apihealth.cache.dto.CacheBenchmarkResponse;
import com.apihealth.cache.dto.CacheStatsResponse;
import com.apihealth.cache.service.CacheService;
import com.apihealth.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache & Performance", description = "In-memory & Redis cache monitoring, metrics, and benchmarks")
@SecurityRequirement(name = "Bearer Authentication")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get cache statistics (hit rate, misses, evictions, Redis health)")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getStats() {
        CacheStatsResponse stats = cacheService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/clear")
    @Operation(summary = "Clear all in-memory and Redis caches")
    public ResponseEntity<ApiResponse<String>> clear() {
        cacheService.clear();
        return ResponseEntity.ok(ApiResponse.success("Cache cleared successfully", "OK"));
    }

    @GetMapping("/benchmark")
    @Operation(summary = "Run latency benchmark comparing uncached vs in-memory vs Redis caching")
    public ResponseEntity<ApiResponse<CacheBenchmarkResponse>> runBenchmark(
            @RequestParam(defaultValue = "100") int iterations) {
        CacheBenchmarkResponse benchmark = cacheService.runBenchmark(iterations);
        return ResponseEntity.ok(ApiResponse.success("Benchmark completed", benchmark));
    }
}
