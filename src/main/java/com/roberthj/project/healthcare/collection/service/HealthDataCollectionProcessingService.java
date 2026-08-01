package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.processor.HealthDataProcessor;
import com.roberthj.project.healthcare.collection.processor.HealthDataProcessorRegistry;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.COLLECTION_REQUEST_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class HealthDataCollectionProcessingService {

    private final HealthDataCollectionRequestRepository collectionRequestRepository;
    private final HealthDataProcessorRegistry processorRegistry;

    @Transactional
    public void process(Long requestId) {
        process(requestId, false);
    }

    @Transactional
    public void process(Long requestId, boolean manual) {
        HealthDataCollectionRequestEntity request = getRequest(requestId);
        HealthDataProcessor processor = getProcessor(request);

        if (manual) {
            processor.reprocess(request);
        } else {
            processor.process(request);
        }
        request.complete();
    }

    private HealthDataCollectionRequestEntity getRequest(Long requestId) {
        return collectionRequestRepository.findById(requestId)
            .orElseThrow(() -> new CollectionException(COLLECTION_REQUEST_NOT_FOUND, requestId.toString()));
    }

    private HealthDataProcessor getProcessor(HealthDataCollectionRequestEntity request) {
        return processorRegistry.getProcessor(request.getSource(), request.getDataType());
    }
}
