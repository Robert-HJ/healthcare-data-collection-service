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
        // 1. 선점된 수집 요청과 Source, Data Type 조합에 맞는 처리기를 조회
        HealthDataCollectionRequestEntity request = getRequest(requestId);
        HealthDataProcessor processor = getProcessor(request);

        // 2. 수동 재처리는 최신 요청으로 저장된 활동 데이터를 덮어쓰지 않는 별도 경로로 처리
        if (manual) {
            processor.reprocess(request);
        } else {
            processor.process(request);
        }

        // 3. 활동 데이터와 집계 저장이 성공한 경우 같은 트랜잭션에서 요청을 완료 처리
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
