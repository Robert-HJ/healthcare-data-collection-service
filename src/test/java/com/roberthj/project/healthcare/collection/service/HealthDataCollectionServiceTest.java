package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import com.roberthj.project.healthcare.collection.validator.CollectionPayloadMetadata;
import com.roberthj.project.healthcare.collection.validator.CollectionPayloadValidator;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import com.roberthj.project.healthcare.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;

import static com.roberthj.project.healthcare.collection.enums.CollectionRequestStatus.PENDING;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.RECORD_KEY_ACCESS_DENIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionServiceTest {

    @Mock
    private CollectionPayloadValidator payloadValidator;

    @Mock
    private MemberService memberService;

    @Mock
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @InjectMocks
    private HealthDataCollectionService collectionService;

    @Test
    void saveValidatedPayloadAsPendingRequest() {
        Long memberId = 1L;
        String recordKey = "record-key";
        JsonNode payload = mock(JsonNode.class);
        CollectionPayloadMetadata metadata = new CollectionPayloadMetadata(
            recordKey,
            HealthDataType.STEPS,
            HealthDataSource.SAMSUNG_HEALTH
        );
        MemberEntity member = MemberEntity.create(
            "홍길동",
            "길동",
            "member@example.com",
            "encoded-password",
            recordKey
        );
        HealthDataCollectionRequestEntity savedRequest = mock(HealthDataCollectionRequestEntity.class);

        when(payloadValidator.validateMetadata(payload)).thenReturn(metadata);
        when(memberService.getMember(memberId)).thenReturn(member);
        when(collectionRequestRepository.save(any(HealthDataCollectionRequestEntity.class)))
            .thenReturn(savedRequest);
        when(savedRequest.getId()).thenReturn(10L);
        when(savedRequest.getStatus()).thenReturn(PENDING);

        HealthDataCollectionResponse response = collectionService.collect(
            memberId,
            recordKey,
            payload
        );

        ArgumentCaptor<HealthDataCollectionRequestEntity> requestCaptor =
            ArgumentCaptor.forClass(HealthDataCollectionRequestEntity.class);
        verify(payloadValidator).validateEntries(metadata.format(), payload);
        verify(collectionRequestRepository).save(requestCaptor.capture());

        HealthDataCollectionRequestEntity request = requestCaptor.getValue();
        assertThat(request.getMember()).isSameAs(member);
        assertThat(request.getDataType()).isEqualTo(metadata.dataType());
        assertThat(request.getSource()).isEqualTo(metadata.source());
        assertThat(request.getPayload()).isSameAs(payload);
        assertThat(request.getStatus()).isEqualTo(PENDING);
        assertThat(response.requestId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(PENDING);
    }

    @Test
    void rejectPayloadWithDifferentRecordKeyBeforeEntryValidation() {
        JsonNode payload = mock(JsonNode.class);
        CollectionPayloadMetadata metadata = new CollectionPayloadMetadata(
            "other-record-key",
            HealthDataType.STEPS,
            HealthDataSource.SAMSUNG_HEALTH
        );

        when(payloadValidator.validateMetadata(payload)).thenReturn(metadata);

        assertThatThrownBy(() -> collectionService.collect(1L, "user-record-key", payload))
            .isInstanceOfSatisfying(CollectionException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(RECORD_KEY_ACCESS_DENIED)
            );

        verify(payloadValidator, never()).validateEntries(any(), any());
        verifyNoInteractions(memberService, collectionRequestRepository);
    }

    @Test
    void rejectPayloadWhenUserRecordKeyIsMissing() {
        JsonNode payload = mock(JsonNode.class);
        CollectionPayloadMetadata metadata = new CollectionPayloadMetadata(
            "payload-record-key",
            HealthDataType.STEPS,
            HealthDataSource.SAMSUNG_HEALTH
        );

        when(payloadValidator.validateMetadata(payload)).thenReturn(metadata);

        assertThatThrownBy(() -> collectionService.collect(1L, null, payload))
            .isInstanceOfSatisfying(CollectionException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(RECORD_KEY_ACCESS_DENIED)
            );

        verify(payloadValidator, never()).validateEntries(any(), any());
        verifyNoInteractions(memberService, collectionRequestRepository);
    }
}
