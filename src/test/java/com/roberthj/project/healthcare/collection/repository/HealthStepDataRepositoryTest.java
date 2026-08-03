package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.entity.HealthStepDailyAggregationEntity;
import com.roberthj.project.healthcare.collection.entity.HealthStepDataEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.config.JpaConfig;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.node.JsonNodeFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(JpaConfig.class)
class HealthStepDataRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Autowired
    private HealthStepDataRepository stepDataRepository;

    @Autowired
    private HealthStepDailyAggregationRepository dailyAggregationRepository;

    @Test
    void saveStepDataAndDailyAggregation() {
        String recordKey = UUID.randomUUID().toString();
        MemberEntity member = memberRepository.save(MemberEntity.create(
            "홍길동", "길동", "member-" + recordKey + "@example.com", "encoded-password", recordKey));
        HealthDataCollectionRequestEntity request = collectionRequestRepository.save(HealthDataCollectionRequestEntity.create(
            member, HealthDataType.STEPS, HealthDataSource.SAMSUNG_HEALTH, JsonNodeFactory.instance.objectNode()));

        HealthStepDataEntity stepData = stepDataRepository.saveAndFlush(
            HealthStepDataEntity.create(
                member,
                request,
                HealthDataSource.SAMSUNG_HEALTH,
                Instant.parse("2026-07-30T01:00:00Z"),
                Instant.parse("2026-07-30T01:10:00Z"),
                new BigDecimal("120.5"),
                new BigDecimal("0.08"),
                new BigDecimal("4.2")
            )
        );
        HealthStepDailyAggregationEntity dailyAggregation = dailyAggregationRepository.saveAndFlush(
            HealthStepDailyAggregationEntity.create(member, "Asia/Seoul", HealthDataSource.SAMSUNG_HEALTH,
                LocalDate.of(2026, 7, 30), new BigDecimal("120.5"), new BigDecimal("0.08"), new BigDecimal("4.2")));

        assertThat(stepData.getId()).isNotNull();
        assertThat(stepData.getSteps()).isEqualByComparingTo("120.5");
        assertThat(dailyAggregation.getId()).isNotNull();
        assertThat(dailyAggregation.getSteps()).isEqualByComparingTo("120.5");
    }
}
