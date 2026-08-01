package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.entity.HealthStepDataEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import({JpaConfig.class, HealthStepDataJdbcRepository.class})
class HealthStepDataJdbcRepositoryTest {

    private static final Instant FIRST_STARTED_AT = Instant.parse("2026-07-30T01:00:00Z");
    private static final Instant FIRST_ENDED_AT = Instant.parse("2026-07-30T01:10:00Z");
    private static final Instant SECOND_STARTED_AT = Instant.parse("2026-07-30T01:10:00Z");
    private static final Instant SECOND_ENDED_AT = Instant.parse("2026-07-30T01:20:00Z");

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Autowired
    private HealthStepDataJdbcRepository stepDataJdbcRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void insertAllWithBatch() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity request = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, request, FIRST_STARTED_AT, FIRST_ENDED_AT, "100", "0.08", "4.2"),
            row(member, request, SECOND_STARTED_AT, SECOND_ENDED_AT, "200", "0.16", "8.4")
        ));

        List<HealthStepDataEntity> insertedData = findAllStepData(member.getId());
        assertThat(insertedData).hasSize(2);
        assertThat(insertedData.get(0).getSteps()).isEqualByComparingTo("100");
        assertThat(insertedData.get(1).getSteps()).isEqualByComparingTo("200");
    }

    @Test
    void updateAllValuesOfExistingInterval() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity olderRequest = saveRequest(member);
        HealthDataCollectionRequestEntity newerRequest = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, olderRequest, FIRST_STARTED_AT, FIRST_ENDED_AT, "100", "0.08", "4.2")
        ));
        stepDataJdbcRepository.upsertAll(List.of(
            row(member, newerRequest, FIRST_STARTED_AT, FIRST_ENDED_AT, "300", "0.24", "12.6")
        ));

        HealthStepDataEntity updatedData = findFirstStepData(member.getId());
        assertThat(updatedData.getCollectionRequest().getId()).isEqualTo(newerRequest.getId());
        assertThat(updatedData.getSteps()).isEqualByComparingTo("300");
        assertThat(updatedData.getDistance()).isEqualByComparingTo("0.24");
        assertThat(updatedData.getCalories()).isEqualByComparingTo("12.6");
    }

    @Test
    void updateAllValuesWhenRetryingSameRequest() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity request = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, request, FIRST_STARTED_AT, FIRST_ENDED_AT, "100", "0.08", "4.2")
        ));
        stepDataJdbcRepository.upsertAllForManualRetry(List.of(
            row(member, request, FIRST_STARTED_AT, FIRST_ENDED_AT, "350", "0.28", "14.7")
        ));

        HealthStepDataEntity reprocessedData = findFirstStepData(member.getId());
        assertThat(reprocessedData.getCollectionRequest().getId()).isEqualTo(request.getId());
        assertThat(reprocessedData.getSteps()).isEqualByComparingTo("350");
        assertThat(reprocessedData.getDistance()).isEqualByComparingTo("0.28");
        assertThat(reprocessedData.getCalories()).isEqualByComparingTo("14.7");
    }

    @Test
    void keepAllValuesWhenRetryingOlderRequest() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity olderRequest = saveRequest(member);
        HealthDataCollectionRequestEntity newerRequest = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, newerRequest, FIRST_STARTED_AT, FIRST_ENDED_AT, "350", "0.28", "14.7")
        ));
        stepDataJdbcRepository.upsertAllForManualRetry(List.of(
            row(member, olderRequest, FIRST_STARTED_AT, FIRST_ENDED_AT, "50", "0.04", "2.1")
        ));

        HealthStepDataEntity keptData = findFirstStepData(member.getId());
        assertThat(keptData.getCollectionRequest().getId()).isEqualTo(newerRequest.getId());
        assertThat(keptData.getSteps()).isEqualByComparingTo("350");
        assertThat(keptData.getDistance()).isEqualByComparingTo("0.28");
        assertThat(keptData.getCalories()).isEqualByComparingTo("14.7");
    }

    @Test
    void insertSeparatelyWhenStartedAtIsSameAndEndedAtIsDifferent() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity request = saveRequest(member);

        stepDataJdbcRepository.upsertAll(List.of(
            row(member, request, FIRST_STARTED_AT, FIRST_ENDED_AT, "100", "0.08", "4.2"),
            row(member, request, FIRST_STARTED_AT, SECOND_ENDED_AT, "200", "0.16", "8.4")
        ));

        List<HealthStepDataEntity> insertedData = findAllStepData(member.getId());
        assertThat(insertedData).hasSize(2);
        assertThat(insertedData)
            .extracting(HealthStepDataEntity::getEndedAt)
            .containsExactlyInAnyOrder(FIRST_ENDED_AT, SECOND_ENDED_AT);
    }

    private MemberEntity saveMember() {
        String recordKey = UUID.randomUUID().toString();
        return memberRepository.saveAndFlush(
            MemberEntity.create(
                "홍길동",
                "길동",
                "member-" + recordKey + "@example.com",
                "encoded-password",
                recordKey
            )
        );
    }

    private HealthDataCollectionRequestEntity saveRequest(MemberEntity member) {
        return collectionRequestRepository.saveAndFlush(
            HealthDataCollectionRequestEntity.create(
                member,
                HealthDataType.STEPS,
                HealthDataSource.SAMSUNG_HEALTH,
                JsonNodeFactory.instance.objectNode()
            )
        );
    }

    private HealthStepDataUpsertRow row(
        MemberEntity member,
        HealthDataCollectionRequestEntity request,
        Instant startedAt,
        Instant endedAt,
        String steps,
        String distance,
        String calories
    ) {
        return new HealthStepDataUpsertRow(
            member.getId(),
            request.getId(),
            HealthDataSource.SAMSUNG_HEALTH,
            startedAt,
            endedAt,
            new BigDecimal(steps),
            new BigDecimal(distance),
            new BigDecimal(calories)
        );
    }

    private List<HealthStepDataEntity> findAllStepData(Long memberId) {
        entityManager.clear();
        return entityManager.createQuery("""
                SELECT stepData
                FROM HealthStepDataEntity stepData
                WHERE stepData.member.id = :memberId
                ORDER BY stepData.startedAt
                """, HealthStepDataEntity.class)
            .setParameter("memberId", memberId)
            .getResultList();
    }

    private HealthStepDataEntity findFirstStepData(Long memberId) {
        return findAllStepData(memberId).get(0);
    }
}
