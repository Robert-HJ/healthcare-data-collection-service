package com.roberthj.project.healthcare.collection.worker;

import com.roberthj.project.healthcare.collection.event.HealthDataCollectionRequestSavedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionProcessingTriggerTest {

    @Mock
    private HealthDataCollectionWorker worker;

    private HealthDataCollectionProcessingTrigger trigger;

    @BeforeEach
    void setUp() {
        trigger = new HealthDataCollectionProcessingTrigger(worker);
    }

    @Test
    void requestProcessingWhenSavedEventIsHandled() {
        trigger.handle(new HealthDataCollectionRequestSavedEvent(1L));

        verify(worker).requestProcessing();
    }

    @Test
    void requestProcessingOnPollingSchedule() {
        trigger.poll();

        verify(worker).requestProcessing();
    }
}
