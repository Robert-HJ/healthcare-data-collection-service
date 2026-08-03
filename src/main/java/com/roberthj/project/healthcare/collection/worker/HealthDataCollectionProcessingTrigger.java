package com.roberthj.project.healthcare.collection.worker;

import com.roberthj.project.healthcare.collection.event.HealthDataCollectionRequestSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class HealthDataCollectionProcessingTrigger {

    private final HealthDataCollectionWorker worker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HealthDataCollectionRequestSavedEvent event) {
        // 1. 수집 요청 저장이 커밋된 이후 즉시 후속 처리를 요청
        worker.requestProcessing();
    }

    @Scheduled(fixedDelayString = "${healthcare.collection.worker.polling-interval}")
    public void poll() {
        // 1. 이벤트 유실이나 미처리 요청을 주기적으로 다시 확인
        worker.requestProcessing();
    }
}
