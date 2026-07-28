package com.roberthj.project.healthcare.collection.repository;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
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
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
@Import(JpaConfig.class)
class HealthDataCollectionRequestRepositoryTest {

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveAndLoadJsonPayload() {
        String recordKey = UUID.randomUUID().toString();
        MemberEntity member = memberRepository.save(
            MemberEntity.create(
                "홍길동",
                "길동",
                "member-" + recordKey + "@example.com",
                "encoded-password",
                recordKey
            )
        );
        ObjectNode payload = createPayload(recordKey);
        HealthDataCollectionRequestEntity request = HealthDataCollectionRequestEntity.create(
            member,
            HealthDataType.STEPS,
            HealthDataSource.SAMSUNG_HEALTH,
            payload
        );

        HealthDataCollectionRequestEntity savedRequest = collectionRequestRepository.saveAndFlush(request);
        Long requestId = savedRequest.getId();
        entityManager.clear();

        HealthDataCollectionRequestEntity loadedRequest = collectionRequestRepository.findById(requestId)
            .orElseThrow();

        assertThat(loadedRequest.getPayload()).isEqualTo(payload);
    }

    private ObjectNode createPayload(String recordKey) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("recordKey", recordKey);
        payload.put("dataType", "steps");
        payload.put("source", "SamsungHealth");
        payload.putArray("data")
            .addObject()
            .put("from", "2026-07-27 10:00:00")
            .put("to", "2026-07-27 10:10:00")
            .put("steps", "123.5");
        return payload;
    }
}
