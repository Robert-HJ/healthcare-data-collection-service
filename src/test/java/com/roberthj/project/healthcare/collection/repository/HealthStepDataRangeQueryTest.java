package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepAggregationSourceRow;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import({JpaConfig.class, HealthStepDataJdbcRepository.class})
class HealthStepDataRangeQueryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Autowired
    private HealthStepDataRepository stepDataRepository;

    @Autowired
    private HealthStepDataJdbcRepository stepDataJdbcRepository;

    @Test
    void findDataWithinStartedAtRangeByMemberAndSource() {
        MemberEntity member = saveMember();
        HealthDataCollectionRequestEntity samsungRequest = saveRequest(member, HealthDataSource.SAMSUNG_HEALTH);
        HealthDataCollectionRequestEntity healthKitRequest = saveRequest(member, HealthDataSource.HEALTH_KIT);

        stepDataJdbcRepository.upsertAll(List.of(
            stepData(member, samsungRequest, HealthDataSource.SAMSUNG_HEALTH,
                "2026-07-30T00:00:00Z", "100.5", "0.08", "4.2"),
            stepData(member, samsungRequest, HealthDataSource.SAMSUNG_HEALTH,
                "2026-07-30T23:50:00Z", "200.25", "0.16", "8.4"),
            stepData(member, samsungRequest, HealthDataSource.SAMSUNG_HEALTH,
                "2026-07-31T00:00:00Z", "400", "0.32", "16.8"),
            stepData(member, healthKitRequest, HealthDataSource.HEALTH_KIT,
                "2026-07-30T12:00:00Z", "800", "0.64", "33.6")
        ));

        List<HealthStepAggregationSourceRow> rows = stepDataRepository.findByStartedAtRange(
            member.getId(), HealthDataSource.SAMSUNG_HEALTH,
            Instant.parse("2026-07-30T00:00:00Z"), Instant.parse("2026-07-31T00:00:00Z"));

        assertThat(rows).extracting(HealthStepAggregationSourceRow::startedAt)
            .containsExactly(Instant.parse("2026-07-30T00:00:00Z"), Instant.parse("2026-07-30T23:50:00Z"));
        assertThat(rows).extracting(row -> row.steps().stripTrailingZeros())
            .containsExactly(new BigDecimal("100.5"), new BigDecimal("200.25"));
    }

    private MemberEntity saveMember() {
        String recordKey = UUID.randomUUID().toString();
        return memberRepository.saveAndFlush(
            MemberEntity.create("홍길동", "길동", "member-" + recordKey + "@example.com", "encoded-password", recordKey));
    }

    private HealthDataCollectionRequestEntity saveRequest(MemberEntity member, HealthDataSource source) {
        return collectionRequestRepository.saveAndFlush(
            HealthDataCollectionRequestEntity.create(member, HealthDataType.STEPS, source, JsonNodeFactory.instance.objectNode()));
    }

    private HealthStepDataUpsertRow stepData(MemberEntity member, HealthDataCollectionRequestEntity request, HealthDataSource source,
                                             String startedAt, String steps, String distance, String calories) {
        Instant startedAtInstant = Instant.parse(startedAt);
        return new HealthStepDataUpsertRow(member.getId(), request.getId(), source, startedAtInstant, startedAtInstant.plusSeconds(600),
            new BigDecimal(steps), new BigDecimal(distance), new BigDecimal(calories));
    }
}
