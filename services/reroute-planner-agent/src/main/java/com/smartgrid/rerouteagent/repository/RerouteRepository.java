package com.smartgrid.rerouteagent.repository;

import com.smartgrid.rerouteagent.domain.Reroute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RerouteRepository extends JpaRepository<Reroute, UUID> {
    List<Reroute> findByDisruptionIdOrderByCreatedAtDesc(String disruptionId);
}
