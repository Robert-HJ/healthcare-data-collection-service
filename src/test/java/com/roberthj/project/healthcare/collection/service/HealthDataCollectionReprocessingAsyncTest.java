package com.roberthj.project.healthcare.collection.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("local")
class HealthDataCollectionReprocessingAsyncTest {

    @Autowired
    private HealthDataCollectionReprocessingService reprocessingService;

    @MockitoBean
    private HealthDataCollectionRequestStateService requestStateService;

    @MockitoBean
    private HealthDataCollectionProcessingService processingService;

    @Test
    void executeManualReprocessingOnCommonAsyncExecutor() {
        AtomicReference<String> threadName = new AtomicReference<>();
        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            return null;
        }).when(processingService).process(1L, true);

        reprocessingService.reprocess(1L);

        verify(processingService, timeout(1000)).process(1L, true);
        assertThat(threadName.get()).startsWith("common-async-");
    }
}
