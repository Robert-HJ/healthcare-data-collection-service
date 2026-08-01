package com.roberthj.project.healthcare.collection.aggregation;

import java.math.BigDecimal;
import java.time.Instant;

public record HealthStepAggregationSourceRow(Instant startedAt, BigDecimal steps, BigDecimal distance, BigDecimal calories) {
}
