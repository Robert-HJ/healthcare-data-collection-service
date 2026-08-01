package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;

import java.math.BigDecimal;
import java.time.Instant;

public record HealthStepDataUpsertRow(
    Long memberId,
    Long collectionRequestId,
    HealthDataSource source,
    Instant startedAt,
    Instant endedAt,
    BigDecimal steps,
    BigDecimal distance,
    BigDecimal calories
) {
}
