package com.roberthj.project.healthcare.collection.api;

import com.roberthj.project.healthcare.auth.component.AccessTokenIssuer;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequest;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import com.roberthj.project.healthcare.member.entity.Member;
import com.roberthj.project.healthcare.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import static com.roberthj.project.healthcare.collection.enums.CollectionRequestStatus.PENDING;
import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.SAMSUNG_HEALTH;
import static com.roberthj.project.healthcare.collection.enums.HealthDataType.STEPS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class HealthDataCollectionControllerTest {

    private static final String FIXTURE_PATH =
        "/fixtures/collection/samsung-health-valid.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Test
    void acceptValidatedPayloadAndSavePendingRequest() throws Exception {
        Member member = saveMember();
        String accessToken = accessTokenIssuer
            .issue(member.getId(), member.getRecordKey())
            .value();
        ObjectNode payload = loadPayload();
        payload.put("recordkey", member.getRecordKey());

        String responseBody = mockMvc.perform(post("/api/health-data/collections")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.requestId").isNumber())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long requestId = objectMapper.readTree(responseBody)
            .get("requestId")
            .longValue();
        HealthDataCollectionRequest savedRequest = collectionRequestRepository
            .findById(requestId)
            .orElseThrow();

        assertThat(savedRequest.getMember().getId()).isEqualTo(member.getId());
        assertThat(savedRequest.getDataType()).isEqualTo(STEPS);
        assertThat(savedRequest.getSource()).isEqualTo(SAMSUNG_HEALTH);
        assertThat(savedRequest.getPayload()).isEqualTo(payload);
        assertThat(savedRequest.getStatus()).isEqualTo(PENDING);
    }

    @Test
    void rejectPayloadWithDifferentRecordKey() throws Exception {
        long requestCount = collectionRequestRepository.count();
        Member member = saveMember();
        String accessToken = accessTokenIssuer
            .issue(member.getId(), member.getRecordKey())
            .value();
        ObjectNode payload = loadPayload();
        payload.put("recordkey", "other-record-key");

        mockMvc.perform(post("/api/health-data/collections")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("COLLECTION002"));

        assertThat(collectionRequestRepository.count()).isEqualTo(requestCount);
    }

    private Member saveMember() {
        String recordKey = UUID.randomUUID().toString();
        return memberRepository.save(
            Member.create(
                "홍길동",
                "길동",
                "member-" + recordKey + "@example.com",
                "encoded-password",
                recordKey
            )
        );
    }

    private ObjectNode loadPayload() throws JacksonException {
        InputStream inputStream = Objects.requireNonNull(
            getClass().getResourceAsStream(FIXTURE_PATH)
        );
        return (ObjectNode) objectMapper.readTree(inputStream);
    }
}
