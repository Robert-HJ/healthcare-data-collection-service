package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthDataCollectionRequestRepository
    extends JpaRepository<HealthDataCollectionRequest, Long> {
}
