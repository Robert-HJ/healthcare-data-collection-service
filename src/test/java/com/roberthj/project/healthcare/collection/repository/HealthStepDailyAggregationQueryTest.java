package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationRow;
import com.roberthj.project.healthcare.collection.aggregation.HealthStepMonthlyAggregationRow;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.HEALTH_KIT;
import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.SAMSUNG_HEALTH;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class HealthStepDailyAggregationQueryTest {

    @Autowired
    private HealthStepDailyAggregationJdbcRepository aggregationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void queryDailyAggregationsWithinMonthBySource() {
        Long memberId = saveMember().getId();
        saveAggregations(memberId);

        List<HealthStepDailyAggregationRow> rows = aggregationRepository.findDaily(
            memberId, "Asia/Seoul", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));

        assertThat(rows).usingRecursiveComparison().withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .isEqualTo(List.of(
                dailyRow("2026-07-01", HEALTH_KIT, "1.2", "0.1", "0"),
                dailyRow("2026-07-01", SAMSUNG_HEALTH, "10.4", "1", "2"),
                dailyRow("2026-07-02", SAMSUNG_HEALTH, "20.4", "2", "3")
            ));
    }

    @Test
    void sumMonthlyAggregationsFromPreciseDailyValuesBySource() {
        Long memberId = saveMember().getId();
        saveAggregations(memberId);

        List<HealthStepMonthlyAggregationRow> rows = aggregationRepository.findMonthly(
            memberId, "Asia/Seoul", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        assertThat(rows).usingRecursiveComparison().withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .isEqualTo(List.of(
                monthlyRow(7, HEALTH_KIT, "1.2", "0.1", "0"),
                monthlyRow(7, SAMSUNG_HEALTH, "30.8", "3", "5"),
                monthlyRow(8, SAMSUNG_HEALTH, "100", "10", "20")
            ));
    }

    private void saveAggregations(Long memberId) {
        aggregationRepository.upsertAll(List.of(
            upsertRow(memberId, "2026-07-01", HEALTH_KIT, "1.2", "0.1", "0"),
            upsertRow(memberId, "2026-07-01", SAMSUNG_HEALTH, "10.4", "1", "2"),
            upsertRow(memberId, "2026-07-02", SAMSUNG_HEALTH, "20.4", "2", "3"),
            upsertRow(memberId, "2026-08-01", SAMSUNG_HEALTH, "100", "10", "20"),
            new HealthStepDailyAggregationUpsertRow(memberId, "UTC", SAMSUNG_HEALTH,
                LocalDate.of(2026, 7, 1), new BigDecimal("999"), BigDecimal.ZERO, BigDecimal.ZERO)
        ));
    }

    private HealthStepDailyAggregationUpsertRow upsertRow(Long memberId, String date, HealthDataSource source,
                                                          String steps, String distance, String calories) {
        return new HealthStepDailyAggregationUpsertRow(memberId, "Asia/Seoul", source, LocalDate.parse(date),
            new BigDecimal(steps), new BigDecimal(distance), new BigDecimal(calories));
    }

    private HealthStepDailyAggregationRow dailyRow(String date, HealthDataSource source, String steps,
                                                   String distance, String calories) {
        return new HealthStepDailyAggregationRow(LocalDate.parse(date), source,
            new BigDecimal(steps), new BigDecimal(distance), new BigDecimal(calories));
    }

    private HealthStepMonthlyAggregationRow monthlyRow(int month, HealthDataSource source, String steps,
                                                       String distance, String calories) {
        return new HealthStepMonthlyAggregationRow(month, source,
            new BigDecimal(steps), new BigDecimal(distance), new BigDecimal(calories));
    }

    private MemberEntity saveMember() {
        String recordKey = UUID.randomUUID().toString();
        return memberRepository.save(MemberEntity.create(
            "홍길동", "길동", "member-" + recordKey + "@example.com", "encoded-password", recordKey));
    }
}
