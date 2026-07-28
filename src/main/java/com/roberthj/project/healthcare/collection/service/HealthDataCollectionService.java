package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequest;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import com.roberthj.project.healthcare.collection.validator.CollectionPayloadMetadata;
import com.roberthj.project.healthcare.collection.validator.CollectionPayloadValidator;
import com.roberthj.project.healthcare.member.entity.Member;
import com.roberthj.project.healthcare.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.RECORD_KEY_ACCESS_DENIED;

@Service
@RequiredArgsConstructor
public class HealthDataCollectionService {

    private final CollectionPayloadValidator payloadValidator;
    private final MemberService memberService;
    private final HealthDataCollectionRequestRepository collectionRequestRepository;

    @Transactional
    public HealthDataCollectionResponse collect(Long memberId, String userRecordKey, JsonNode payload) {

        // 1. Json 최상위 메타데이터 검증
        CollectionPayloadMetadata metadata = payloadValidator.validateMetadata(payload);

        // 2. Record Key 검증
        checkRecordKey(userRecordKey, metadata.recordKey());

        // 3. 정상 사용자 조회
        Member member = memberService.getMember(memberId);

        // 4. 내부 데이터 검증
        payloadValidator.validateEntries(metadata.source(), payload);

        // 5. 원본 데이터 저장
        HealthDataCollectionRequest request = HealthDataCollectionRequest.create(
            member,
            metadata.dataType(),
            metadata.source(),
            payload
        );

        return HealthDataCollectionResponse.from(
            collectionRequestRepository.save(request)
        );
    }

    private void checkRecordKey(String userRecordKey, String payloadRecordKey) {
        if (userRecordKey == null || !userRecordKey.equals(payloadRecordKey)) {
            throw new CollectionException(RECORD_KEY_ACCESS_DENIED);
        }
    }
}
