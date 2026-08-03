package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationRow;
import com.roberthj.project.healthcare.collection.aggregation.HealthStepMonthlyAggregationRow;
import com.roberthj.project.healthcare.collection.config.HealthStepAggregationProperties;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthStepDailyAggregationJdbcRepository;
import com.roberthj.project.healthcare.collection.response.HealthStepDailyResponse;
import com.roberthj.project.healthcare.collection.response.HealthStepMonthlyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.RECORD_KEY_ACCESS_DENIED;

@Service
@RequiredArgsConstructor
public class HealthStepQueryService {

    private final HealthStepDailyAggregationJdbcRepository aggregationRepository;
    private final HealthStepAggregationProperties aggregationProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<HealthStepDailyResponse> getDaily(Long memberId, String userRecordKey, String recordKey, YearMonth yearMonth) {
        checkRecordKey(userRecordKey, recordKey);
        ZoneId timeZone = aggregationProperties.defaultTimeZone();
        YearMonth targetMonth = yearMonth == null ? YearMonth.now(clock.withZone(timeZone)) : yearMonth;
        LocalDate from = targetMonth.atDay(1);
        LocalDate to = targetMonth.plusMonths(1).atDay(1);

        return aggregationRepository.findDaily(memberId, timeZone.getId(), from, to).stream().map(this::toDailyResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<HealthStepMonthlyResponse> getMonthly(Long memberId, String userRecordKey, String recordKey, Integer year) {
        checkRecordKey(userRecordKey, recordKey);
        ZoneId timeZone = aggregationProperties.defaultTimeZone();
        int targetYear = year == null ? YearMonth.now(clock.withZone(timeZone)).getYear() : year;
        LocalDate from = LocalDate.of(targetYear, 1, 1);
        LocalDate to = from.plusYears(1);

        return aggregationRepository.findMonthly(memberId, timeZone.getId(), from, to).stream()
            .map(row -> toMonthlyResponse(targetYear, row)).toList();
    }

    private void checkRecordKey(String userRecordKey, String recordKey) {
        if (userRecordKey == null || !userRecordKey.equals(recordKey)) {
            throw new CollectionException(RECORD_KEY_ACCESS_DENIED);
        }
    }

    private HealthStepDailyResponse toDailyResponse(HealthStepDailyAggregationRow row) {
        return new HealthStepDailyResponse(row.date(), row.source(), roundSteps(row.steps()), normalize(row.distance()), normalize(row.calories()));
    }

    private HealthStepMonthlyResponse toMonthlyResponse(int year, HealthStepMonthlyAggregationRow row) {
        return new HealthStepMonthlyResponse(YearMonth.of(year, row.month()), row.source(), roundSteps(row.steps()),
            normalize(row.distance()), normalize(row.calories()));
    }

    private long roundSteps(BigDecimal steps) {
        return steps.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}
