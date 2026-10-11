package com.apihealth.alert.repository;

import com.apihealth.alert.entity.Alert;
import com.apihealth.alert.entity.AlertStatus;
import com.apihealth.alert.entity.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("SELECT a FROM Alert a WHERE a.monitoredApi.owner.id = :ownerId ORDER BY a.createdAt DESC")
    Page<Alert> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.id = :id AND a.monitoredApi.owner.id = :ownerId")
    Optional<Alert> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query("SELECT a FROM Alert a WHERE a.monitoredApi.id = :apiId AND a.alertType = :alertType AND a.status = :status")
    List<Alert> findActiveAlertsByApiAndType(
            @Param("apiId") Long apiId,
            @Param("alertType") AlertType alertType,
            @Param("status") AlertStatus status);

    @Query("SELECT a FROM Alert a WHERE a.monitoredApi.id = :apiId AND a.status = :status")
    List<Alert> findActiveAlertsByApiId(@Param("apiId") Long apiId, @Param("status") AlertStatus status);

    @Query("SELECT a FROM Alert a WHERE a.monitoredApi.owner.id = :ownerId ORDER BY a.createdAt DESC")
    List<Alert> findRecentByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
