package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthDataCollectionRequestRepository
    extends JpaRepository<HealthDataCollectionRequestEntity, Long> {
}
