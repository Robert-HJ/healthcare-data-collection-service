package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthActivityDailyAggregationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthActivityDailyAggregationRepository
    extends JpaRepository<HealthActivityDailyAggregationEntity, Long> {
}
