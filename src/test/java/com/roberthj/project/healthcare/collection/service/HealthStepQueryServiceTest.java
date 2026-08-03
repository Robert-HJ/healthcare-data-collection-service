package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationRow;
import com.roberthj.project.healthcare.collection.aggregation.HealthStepMonthlyAggregationRow;
import com.roberthj.project.healthcare.collection.config.HealthStepAggregationProperties;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthStepDailyAggregationJdbcRepository;
import com.roberthj.project.healthcare.collection.response.HealthStepDailyResponse;
import com.roberthj.project.healthcare.collection.response.HealthStepMonthlyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.HEALTH_KIT;
import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.SAMSUNG_HEALTH;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.RECORD_KEY_ACCESS_DENIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthStepQueryServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-31T15:30:00Z"), ZoneOffset.UTC);

    @Mock
    private HealthStepDailyAggregationJdbcRepository aggregationRepository;

    private HealthStepQueryService queryService;

    @BeforeEach
    void setUp() {
        HealthStepAggregationProperties properties = new HealthStepAggregationProperties(List.of(SEOUL, ZoneOffset.UTC), SEOUL);
        queryService = new HealthStepQueryService(aggregationRepository, properties, CLOCK);
    }

    @Test
    void queryDailyAggregationsForRequestedMonth() {
        when(aggregationRepository.findDaily(1L, "Asia/Seoul", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)))
            .thenReturn(List.of(
                new HealthStepDailyAggregationRow(LocalDate.of(2026, 7, 1), SAMSUNG_HEALTH,
                    new BigDecimal("123.5"), new BigDecimal("1.2300"), new BigDecimal("45.600"))
            ));

        List<HealthStepDailyResponse> responses = queryService.getDaily(
            1L, "record-key", "record-key", YearMonth.of(2026, 7));

        assertThat(responses).containsExactly(
            new HealthStepDailyResponse(LocalDate.of(2026, 7, 1), SAMSUNG_HEALTH,
                124L, new BigDecimal("1.23"), new BigDecimal("45.6"))
        );
    }

    @Test
    void queryMonthlyAggregationsForRequestedYear() {
        when(aggregationRepository.findMonthly(1L, "Asia/Seoul", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
            .thenReturn(List.of(
                new HealthStepMonthlyAggregationRow(7, HEALTH_KIT,
                    new BigDecimal("10.6"), new BigDecimal("2.500"), BigDecimal.ZERO)
            ));

        List<HealthStepMonthlyResponse> responses = queryService.getMonthly(
            1L, "record-key", "record-key", 2026);

        assertThat(responses).containsExactly(
            new HealthStepMonthlyResponse(YearMonth.of(2026, 7), HEALTH_KIT,
                11L, new BigDecimal("2.5"), BigDecimal.ZERO)
        );
    }

    @Test
    void useCurrentMonthAndYearInDefaultTimeZoneWhenPeriodIsMissing() {
        when(aggregationRepository.findDaily(1L, "Asia/Seoul", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)))
            .thenReturn(List.of());
        when(aggregationRepository.findMonthly(1L, "Asia/Seoul", LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1)))
            .thenReturn(List.of());

        queryService.getDaily(1L, "record-key", "record-key", null);
        queryService.getMonthly(1L, "record-key", "record-key", null);

        verify(aggregationRepository).findDaily(
            1L, "Asia/Seoul", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1));
        verify(aggregationRepository).findMonthly(
            1L, "Asia/Seoul", LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1));
    }

    @Test
    void rejectDifferentRecordKeyBeforeQueryingAggregations() {
        assertThatThrownBy(() -> queryService.getDaily(1L, "record-key", "other-record-key", null))
            .isInstanceOfSatisfying(CollectionException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(RECORD_KEY_ACCESS_DENIED));

        verify(aggregationRepository, never()).findDaily(
            1L, "Asia/Seoul", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1));
    }
}
