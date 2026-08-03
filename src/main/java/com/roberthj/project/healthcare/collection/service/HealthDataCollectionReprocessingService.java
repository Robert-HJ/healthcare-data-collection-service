package com.roberthj.project.healthcare.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDataCollectionReprocessingService {

    private final HealthDataCollectionRequestStateService requestStateService;
    private final HealthDataCollectionProcessingService processingService;

    @Async("applicationTaskExecutor")
    public void reprocess(Long requestId) {
        try {
            processingService.process(requestId, true);
        } catch (Exception exception) {
            log.error("건강 데이터 수집 요청 수동 재처리에 실패했습니다. requestId={}", requestId, exception);
            recordFailure(requestId, exception);
        }
    }

    private void recordFailure(Long requestId, Exception processingException) {
        try {
            requestStateService.fail(requestId, processingException);
        } catch (Exception exception) {
            log.error("건강 데이터 수집 요청의 실패 상태 저장에 실패했습니다. requestId={}", requestId, exception);
        }
    }
}
