package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthStepDailyAggregationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthStepDailyAggregationRepository
    extends JpaRepository<HealthStepDailyAggregationEntity, Long> {
}
