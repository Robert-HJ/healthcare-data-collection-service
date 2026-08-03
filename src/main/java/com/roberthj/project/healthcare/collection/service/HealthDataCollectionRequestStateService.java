package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.config.HealthDataCollectionWorkerProperties;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestClaimRepository;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.COLLECTION_REQUEST_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class HealthDataCollectionRequestStateService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

    private final HealthDataCollectionRequestRepository collectionRequestRepository;
    private final HealthDataCollectionRequestClaimRepository claimRepository;
    private final HealthDataCollectionWorkerProperties workerProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> claimNext() {
        Optional<Long> requestId = claimRepository.findNextRequest(workerProperties.maxRetryCount(),
            workerProperties.staleProcessingTimeout());
        requestId.ifPresent(claimRepository::updateProcessing);
        return requestId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimForManualReprocessing(Long requestId) {
        return claimRepository.claimForManualReprocessing(requestId, workerProperties.staleProcessingTimeout());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long requestId, Exception exception) {
        HealthDataCollectionRequestEntity request = collectionRequestRepository.findById(requestId)
            .orElseThrow(() -> new CollectionException(COLLECTION_REQUEST_NOT_FOUND, requestId.toString()));
        request.fail(getErrorMessage(exception));
    }

    private String getErrorMessage(Exception exception) {
        String errorMessage = exception.getMessage();
        if (errorMessage == null || errorMessage.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return errorMessage.substring(0, Math.min(errorMessage.length(), ERROR_MESSAGE_MAX_LENGTH));
    }
}
