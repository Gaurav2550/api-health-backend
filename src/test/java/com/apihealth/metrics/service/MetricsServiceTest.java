package com.apihealth.metrics.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsServiceTest {

    @Test
    @DisplayName("Should correctly calculate percentiles using nearest-rank method")
    void testPercentileCalculations() {
        // 10 elements: [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
        List<Long> latencies = List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L);

        // P50: index = ceil(0.50 * 10) - 1 = 5 - 1 = 4 -> 50
        assertEquals(50L, MetricsService.percentile(latencies, 50));

        // P95: index = ceil(0.95 * 10) - 1 = 10 - 1 = 9 -> 100
        assertEquals(100L, MetricsService.percentile(latencies, 95));

        // P99: index = ceil(0.99 * 10) - 1 = 10 - 1 = 9 -> 100
        assertEquals(100L, MetricsService.percentile(latencies, 99));

        // 100 elements: [1..100]
        List<Long> hundred = java.util.stream.LongStream.rangeClosed(1, 100).boxed().toList();
        assertEquals(50L, MetricsService.percentile(hundred, 50));
        assertEquals(95L, MetricsService.percentile(hundred, 95));
        assertEquals(99L, MetricsService.percentile(hundred, 99));
    }

    @Test
    @DisplayName("Should handle single element and empty list percentiles safely")
    void testEdgeCases() {
        assertEquals(0L, MetricsService.percentile(Collections.emptyList(), 95));
        assertEquals(42L, MetricsService.percentile(List.of(42L), 50));
        assertEquals(42L, MetricsService.percentile(List.of(42L), 99));
    }
}
