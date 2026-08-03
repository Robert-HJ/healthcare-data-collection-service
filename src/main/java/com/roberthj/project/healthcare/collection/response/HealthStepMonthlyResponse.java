package com.roberthj.project.healthcare.collection.response;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;

import java.math.BigDecimal;
import java.time.YearMonth;

public record HealthStepMonthlyResponse(
        YearMonth month,
        HealthDataSource source,
        long steps,
        BigDecimal distance,
        BigDecimal calories
) {
}
