package com.roberthj.project.healthcare.collection.aggregation;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthStepDailyAggregationRow(
        LocalDate date,
        HealthDataSource source,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
) {
}
