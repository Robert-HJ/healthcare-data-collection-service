package com.roberthj.project.healthcare.collection.aggregation;

import com.roberthj.project.healthcare.collection.config.HealthStepAggregationConfig;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.entity.HealthStepDailyAggregationEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDailyAggregationJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDailyAggregationRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataUpsertRow;
import com.roberthj.project.healthcare.config.JpaConfig;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.node.JsonNodeFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import({
    JpaConfig.class,
    HealthStepAggregationConfig.class,
    HealthStepDailyAggregationService.class,
    HealthStepDataJdbcRepository.class,
    HealthStepDailyAggregationJdbcRepository.class
})
class HealthStepDailyAggregationServiceTest {

    private static final Instant FIRST_STARTED_AT = Instant.parse("2026-07-29T15:00:00Z");
    private static final Instant SECOND_STARTED_AT = Instant.parse("2026-07-30T00:00:00Z");

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Autowired
    private HealthStepDataJdbcRepository stepDataJdbcRepository;

    @Autowired
    private HealthStepDailyAggregationRepository dailyAggregationRepository;

    @Autowired
    private HealthStepDailyAggregationService aggregationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createDailyAggregationsForAllSupportedTimeZones() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity request = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, request, FIRST_STARTED_AT, "100", "0.08", "4.2"),
            row(member, request, SECOND_STARTED_AT, "200", "0.16", "8.4")
        ));
        aggregationService.recalculate(member, HealthDataSource.SAMSUNG_HEALTH, List.of(FIRST_STARTED_AT, SECOND_STARTED_AT));

        Map<String, HealthStepDailyAggregationEntity> aggregations = findAggregationsByKey();
        assertThat(aggregations).hasSize(3);
        assertAggregation(aggregations.get("Asia/Seoul:2026-07-30"), "300", "0.24", "12.6");
        assertAggregation(aggregations.get("UTC:2026-07-29"), "100", "0.08", "4.2");
        assertAggregation(aggregations.get("UTC:2026-07-30"), "200", "0.16", "8.4");
    }

    @Test
    void recalculateExistingAggregationsFromAllStepData() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity firstRequest = saveRequest(member);
        HealthDataCollectionRequestEntity secondRequest = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, firstRequest, FIRST_STARTED_AT, "100", "0.08", "4.2"),
            row(member, firstRequest, SECOND_STARTED_AT, "200", "0.16", "8.4")
        ));
        aggregationService.recalculate(member, HealthDataSource.SAMSUNG_HEALTH, List.of(FIRST_STARTED_AT, SECOND_STARTED_AT));

        stepDataJdbcRepository.upsertAll(List.of(row(member, secondRequest, FIRST_STARTED_AT, "150", "0.12", "6.3")));
        aggregationService.recalculate(member, HealthDataSource.SAMSUNG_HEALTH, List.of(FIRST_STARTED_AT));

        Map<String, HealthStepDailyAggregationEntity> aggregations = findAggregationsByKey();
        assertThat(aggregations).hasSize(3);
        assertAggregation(aggregations.get("Asia/Seoul:2026-07-30"), "350", "0.28", "14.7");
        assertAggregation(aggregations.get("UTC:2026-07-29"), "150", "0.12", "6.3");
        assertAggregation(aggregations.get("UTC:2026-07-30"), "200", "0.16", "8.4");
    }

    private MemberEntity saveMember() {
        String recordKey = UUID.randomUUID().toString();
        return memberRepository.saveAndFlush(MemberEntity.create("홍길동", "길동", "member-" + recordKey + "@example.com", "encoded-password", recordKey));
    }

    private HealthDataCollectionRequestEntity saveRequest(MemberEntity member) {
        return collectionRequestRepository.saveAndFlush(HealthDataCollectionRequestEntity.create(
            member, HealthDataType.STEPS, HealthDataSource.SAMSUNG_HEALTH, JsonNodeFactory.instance.objectNode()));
    }

    private HealthStepDataUpsertRow row(MemberEntity member, HealthDataCollectionRequestEntity request,
                                        Instant startedAt, String steps, String distance, String calories) {
        return new HealthStepDataUpsertRow(member.getId(), request.getId(), HealthDataSource.SAMSUNG_HEALTH,
            startedAt, startedAt.plusSeconds(600), new BigDecimal(steps), new BigDecimal(distance), new BigDecimal(calories));
    }

    private Map<String, HealthStepDailyAggregationEntity> findAggregationsByKey() {
        entityManager.clear();
        return dailyAggregationRepository.findAll().stream().collect(Collectors.toMap(
            aggregation -> aggregation.getTimezone() + ":" + aggregation.getAggregateDate(), Function.identity()));
    }

    private void assertAggregation(HealthStepDailyAggregationEntity aggregation, String steps, String distance, String calories) {
        assertThat(aggregation).isNotNull();
        assertThat(aggregation.getSteps()).isEqualByComparingTo(steps);
        assertThat(aggregation.getDistance()).isEqualByComparingTo(distance);
        assertThat(aggregation.getCalories()).isEqualByComparingTo(calories);
    }
}
