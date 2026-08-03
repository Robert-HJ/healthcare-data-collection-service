package com.roberthj.project.healthcare.collection.response;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthStepDailyResponse(
        LocalDate date,
        HealthDataSource source,
        long steps,
        BigDecimal distance,
        BigDecimal calories
) {
}
