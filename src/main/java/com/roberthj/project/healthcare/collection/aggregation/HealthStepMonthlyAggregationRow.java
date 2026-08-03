package com.roberthj.project.healthcare.collection.aggregation;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;

import java.math.BigDecimal;

public record HealthStepMonthlyAggregationRow(
        int month,
        HealthDataSource source,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
) {
}
