package com.roberthj.project.healthcare.collection.worker;

import com.roberthj.project.healthcare.collection.config.HealthDataCollectionWorkerProperties;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionProcessingService;
import com.roberthj.project.healthcare.collection.service.HealthDataCollectionRequestStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionWorkerTest {

    @Mock
    private HealthDataCollectionRequestStateService requestStateService;

    @Mock
    private HealthDataCollectionProcessingService processingService;

    private HealthDataCollectionWorker worker;

    @BeforeEach
    void setUp() {
        HealthDataCollectionWorkerProperties properties = new HealthDataCollectionWorkerProperties(1, 5, Duration.ofMinutes(5));
        worker = new HealthDataCollectionWorker(requestStateService, processingService, new SyncTaskExecutor(), properties);
    }

    @Test
    void processRequestsUntilNoRequestRemains() {
        when(requestStateService.claimNext()).thenReturn(Optional.of(1L), Optional.of(2L), Optional.empty());

        worker.requestProcessing();

        verify(processingService).process(1L);
        verify(processingService).process(2L);
    }

    @Test
    void recordFailureAndContinueWithNextRequest() {
        RuntimeException processingException = new RuntimeException("processing failed");
        when(requestStateService.claimNext()).thenReturn(Optional.of(1L), Optional.of(2L), Optional.empty());
        doThrow(processingException).when(processingService).process(1L);

        worker.requestProcessing();

        verify(requestStateService).fail(1L, processingException);
        verify(processingService).process(2L);
    }
}
