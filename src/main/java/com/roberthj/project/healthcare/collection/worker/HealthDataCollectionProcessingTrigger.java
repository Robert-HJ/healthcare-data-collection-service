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
        worker.requestProcessing();
    }

    @Scheduled(fixedDelayString = "${healthcare.collection.worker.polling-interval}")
    public void poll() {
        worker.requestProcessing();
    }
}
