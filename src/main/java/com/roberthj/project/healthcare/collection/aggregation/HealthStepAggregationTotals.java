package com.roberthj.project.healthcare.collection.aggregation;

import java.math.BigDecimal;

public record HealthStepAggregationTotals(BigDecimal steps, BigDecimal distance, BigDecimal calories) {

    public static final HealthStepAggregationTotals ZERO = new HealthStepAggregationTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public HealthStepAggregationTotals {
        steps = steps == null ? BigDecimal.ZERO : steps;
        distance = distance == null ? BigDecimal.ZERO : distance;
        calories = calories == null ? BigDecimal.ZERO : calories;
    }

    public HealthStepAggregationTotals add(HealthStepAggregationSourceRow row) {
        return new HealthStepAggregationTotals(steps.add(row.steps()), distance.add(row.distance()), calories.add(row.calories()));
    }
}
