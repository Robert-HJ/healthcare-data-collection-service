package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthStepDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthStepDataRepository extends JpaRepository<HealthStepDataEntity, Long> {
}
