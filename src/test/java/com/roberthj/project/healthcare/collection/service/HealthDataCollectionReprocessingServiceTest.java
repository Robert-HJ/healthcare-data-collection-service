package com.roberthj.project.healthcare.collection.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionReprocessingServiceTest {

    @Mock
    private HealthDataCollectionRequestStateService requestStateService;

    @Mock
    private HealthDataCollectionProcessingService processingService;

    @InjectMocks
    private HealthDataCollectionReprocessingService reprocessingService;

    @Test
    void reprocessRequest() {
        reprocessingService.reprocess(1L);

        verify(processingService).process(1L, true);
    }

    @Test
    void recordFailureWhenManualReprocessingFails() {
        RuntimeException processingException = new RuntimeException("processing failed");
        doThrow(processingException).when(processingService).process(1L, true);

        reprocessingService.reprocess(1L);

        verify(requestStateService).fail(1L, processingException);
    }
}
