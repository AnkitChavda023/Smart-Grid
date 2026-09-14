package com.smartgrid.disruptionagent.repository;

import com.smartgrid.disruptionagent.domain.Disruption;
import com.smartgrid.disruptionagent.domain.DisruptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DisruptionRepository extends JpaRepository<Disruption, UUID> {

    @Query("SELECT DISTINCT d FROM Disruption d LEFT JOIN FETCH d.affectedSkus WHERE d.status = :status ORDER BY d.createdAt DESC")
    List<Disruption> findByStatusOrderByCreatedAtDesc(@Param("status") DisruptionStatus status);
}
