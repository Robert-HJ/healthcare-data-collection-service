package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthActivityDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthActivityDataRepository extends JpaRepository<HealthActivityDataEntity, Long> {
}
