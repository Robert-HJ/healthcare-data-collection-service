package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepAggregationSourceRow;
import com.roberthj.project.healthcare.collection.entity.HealthStepDataEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface HealthStepDataRepository extends JpaRepository<HealthStepDataEntity, Long> {

    @Query("""
        SELECT new com.roberthj.project.healthcare.collection.aggregation.HealthStepAggregationSourceRow(
            stepData.startedAt,
            stepData.steps,
            stepData.distance,
            stepData.calories
        )
        FROM HealthStepDataEntity stepData
        WHERE stepData.member.id = :memberId
          AND stepData.source = :source
          AND stepData.startedAt >= :rangeStart
          AND stepData.startedAt < :rangeEnd
        ORDER BY stepData.startedAt
        """)
    List<HealthStepAggregationSourceRow> findByStartedAtRange(
        @Param("memberId") Long memberId, @Param("source") HealthDataSource source,
        @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);
}
