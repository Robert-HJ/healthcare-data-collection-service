package com.roberthj.project.healthcare.collection.worker;

import com.roberthj.project.healthcare.collection.config.HealthDataCollectionWorkerProperties;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionProcessingService;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionRequestStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class HealthDataCollectionWorker {

    private final HealthDataCollectionRequestStateService requestStateService;
    private final HealthDataCollectionProcessingService processingService;
    private final TaskExecutor processingExecutor;
    private final int workerConcurrency;

    public HealthDataCollectionWorker(
        HealthDataCollectionRequestStateService requestStateService,
        HealthDataCollectionProcessingService processingService,
        @Qualifier("healthDataCollectionProcessingExecutor") TaskExecutor processingExecutor,
        HealthDataCollectionWorkerProperties workerProperties
    ) {
        this.requestStateService = requestStateService;
        this.processingService = processingService;
        this.processingExecutor = processingExecutor;
        this.workerConcurrency = workerProperties.concurrency();
    }

    public void requestProcessing() {
        for (int count = 0; count < workerConcurrency; count++) {
            try {
                processingExecutor.execute(this::processRequests);
            } catch (TaskRejectedException exception) {
                return;
            } catch (Exception exception) {
                log.error("건강 데이터 수집 작업 요청에 실패했습니다.", exception);
                return;
            }
        }
    }

    private void processRequests() {
        try {
            while (true) {
                Optional<Long> requestId = requestStateService.claimNext();
                if (requestId.isEmpty()) {
                    return;
                }
                processRequest(requestId.get());
            }
        } catch (Exception exception) {
            log.error("건강 데이터 수집 Worker 실행 중 오류가 발생했습니다.", exception);
        }
    }

    private void processRequest(Long requestId) {
        try {
            processingService.process(requestId);
        } catch (Exception exception) {
            log.error("건강 데이터 수집 요청 처리에 실패했습니다. requestId={}", requestId, exception);
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
