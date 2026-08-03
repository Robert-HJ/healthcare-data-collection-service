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
        // 1. 설정된 동시 실행 수만큼 작업 처리기를 요청
        for (int count = 0; count < workerConcurrency; count++) {
            try {
                processingExecutor.execute(this::processRequests);
            } catch (TaskRejectedException exception) {
                // 2. 실행 중인 작업이 스레드 풀을 채우고 있으면 중복 요청을 무시
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
                // 1. 처리 가능한 요청 한 건을 별도 트랜잭션으로 선점
                Optional<Long> requestId = requestStateService.claimNext();

                // 2. 남은 요청이 없으면 현재 작업을 종료
                if (requestId.isEmpty()) {
                    return;
                }

                // 3. 선점한 요청을 처리한 뒤 다음 요청을 계속 조회
                processRequest(requestId.get());
            }
        } catch (Exception exception) {
            log.error("건강 데이터 수집 Worker 실행 중 오류가 발생했습니다.", exception);
        }
    }

    private void processRequest(Long requestId) {
        try {
            // 1. 정규화, 활동 데이터 적재와 집계를 하나의 트랜잭션으로 처리
            processingService.process(requestId);
        } catch (Exception exception) {
            log.error("건강 데이터 수집 요청 처리에 실패했습니다. requestId={}", requestId, exception);

            // 2. 처리 트랜잭션이 롤백되면 실패 상태를 별도 트랜잭션으로 기록
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
