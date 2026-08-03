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
            // 1. 최신 요청 데이터 보호 규칙을 적용하는 수동 재처리 경로 실행
            processingService.process(requestId, true);
        } catch (Exception exception) {
            log.error("건강 데이터 수집 요청 수동 재처리에 실패했습니다. requestId={}", requestId, exception);

            // 2. 비동기 처리 실패를 별도 트랜잭션으로 기록
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
