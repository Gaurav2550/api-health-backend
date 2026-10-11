package com.apihealth.monitoring.repository;

import com.apihealth.monitoring.entity.MonitoredApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredApiRepository extends JpaRepository<MonitoredApi, Long> {

    Page<MonitoredApi> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<MonitoredApi> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    List<MonitoredApi> findByActiveTrue();

    @Query("SELECT m FROM MonitoredApi m WHERE m.owner.id = :ownerId")
    List<MonitoredApi> findAllByOwnerId(@Param("ownerId") Long ownerId);
}
