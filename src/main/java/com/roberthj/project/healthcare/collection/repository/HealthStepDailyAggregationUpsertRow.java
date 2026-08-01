package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthStepDailyAggregationUpsertRow(Long memberId, String timezone, HealthDataSource source, LocalDate aggregateDate,
                                                  BigDecimal steps, BigDecimal distance, BigDecimal calories) {
}
