package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.COLLECTION_REQUEST_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class HealthDataCollectionAdminService {

    private final HealthDataCollectionRequestRepository collectionRequestRepository;
    private final HealthDataCollectionReprocessingService reprocessingService;
    private final HealthDataCollectionRequestStateService requestStateService;

    @Transactional(readOnly = true)
    public HealthDataCollectionResponse getStatus(Long requestId) {
        return HealthDataCollectionResponse.from(getRequest(requestId));
    }

    public void reprocess(Long requestId) {
        // 1. 수동 재처리 대상 요청이 존재하는지 확인
        if (!collectionRequestRepository.existsById(requestId)) {
            throw new CollectionException(COLLECTION_REQUEST_NOT_FOUND, requestId.toString());
        }

        // 2. 현재 처리 중이지 않은 요청만 수동 재처리 대상으로 선점
        if (!requestStateService.claimForManualReprocessing(requestId)) {
            return;
        }

        try {
            // 3. API 응답과 실제 처리를 분리하기 위해 비동기 재처리를 요청
            reprocessingService.reprocess(requestId);
        } catch (TaskRejectedException exception) {
            // 4. 작업 제출 자체가 거절되면 선점된 요청을 실패 상태로 복구
            requestStateService.fail(requestId, exception);
            throw exception;
        }
    }

    private HealthDataCollectionRequestEntity getRequest(Long requestId) {
        return collectionRequestRepository.findById(requestId)
            .orElseThrow(() -> new CollectionException(COLLECTION_REQUEST_NOT_FOUND, requestId.toString()));
    }
}
