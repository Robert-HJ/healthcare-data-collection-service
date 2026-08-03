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
        if (!collectionRequestRepository.existsById(requestId)) {
            throw new CollectionException(COLLECTION_REQUEST_NOT_FOUND, requestId.toString());
        }
        if (!requestStateService.claimForManualReprocessing(requestId)) {
            return;
        }

        try {
            reprocessingService.reprocess(requestId);
        } catch (TaskRejectedException exception) {
            requestStateService.fail(requestId, exception);
            throw exception;
        }
    }

    private HealthDataCollectionRequestEntity getRequest(Long requestId) {
        return collectionRequestRepository.findById(requestId)
            .orElseThrow(() -> new CollectionException(COLLECTION_REQUEST_NOT_FOUND, requestId.toString()));
    }
}
