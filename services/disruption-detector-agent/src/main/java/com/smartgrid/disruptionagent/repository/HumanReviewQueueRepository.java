package com.smartgrid.disruptionagent.repository;

import com.smartgrid.disruptionagent.domain.HumanReviewQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HumanReviewQueueRepository extends JpaRepository<HumanReviewQueueEntry, UUID> {
}
