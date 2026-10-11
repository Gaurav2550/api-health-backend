package com.apihealth.monitoring.repository;

import com.apihealth.monitoring.entity.HealthCheck;
import com.apihealth.monitoring.entity.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {

    Page<HealthCheck> findByMonitoredApiIdOrderByCheckedAtDesc(Long apiId, Pageable pageable);

    @Query("SELECT h FROM HealthCheck h WHERE h.monitoredApi.id = :apiId AND h.checkedAt >= :since ORDER BY h.checkedAt DESC")
    List<HealthCheck> findByApiIdSince(@Param("apiId") Long apiId, @Param("since") Instant since);

    @Query("SELECT h FROM HealthCheck h WHERE h.monitoredApi.id = :apiId AND h.checkedAt BETWEEN :start AND :end ORDER BY h.checkedAt ASC")
    List<HealthCheck> findByApiIdBetween(@Param("apiId") Long apiId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(h) FROM HealthCheck h WHERE h.monitoredApi.id = :apiId AND h.checkedAt >= :since")
    long countByApiIdSince(@Param("apiId") Long apiId, @Param("since") Instant since);

    @Query("SELECT COUNT(h) FROM HealthCheck h WHERE h.monitoredApi.id = :apiId AND h.status = :status AND h.checkedAt >= :since")
    long countByApiIdAndStatusSince(@Param("apiId") Long apiId, @Param("status") HealthStatus status, @Param("since") Instant since);

    @Query("SELECT h.responseTimeMillis FROM HealthCheck h WHERE h.monitoredApi.id = :apiId AND h.checkedAt >= :since AND h.responseTimeMillis IS NOT NULL ORDER BY h.responseTimeMillis ASC")
    List<Long> findLatenciesByApiIdSince(@Param("apiId") Long apiId, @Param("since") Instant since);

    @Query("SELECT h FROM HealthCheck h WHERE h.monitoredApi.id = :apiId ORDER BY h.checkedAt DESC")
    List<HealthCheck> findRecentByApiId(@Param("apiId") Long apiId, Pageable pageable);

    @Query("SELECT COUNT(h) FROM HealthCheck h WHERE h.monitoredApi.id = :apiId")
    long countByApiId(@Param("apiId") Long apiId);
}
