package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.processor.HealthDataProcessor;
import com.roberthj.project.healthcare.collection.processor.HealthDataProcessorRegistry;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.COLLECTION_REQUEST_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionProcessingServiceTest {

    @Mock
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Mock
    private HealthDataProcessorRegistry processorRegistry;

    @Mock
    private HealthDataProcessor processor;

    @Mock
    private HealthDataCollectionRequestEntity request;

    @InjectMocks
    private HealthDataCollectionProcessingService processingService;

    @Test
    void processCollectionRequestAndComplete() {
        prepareRequest();

        processingService.process(1L);

        verify(processor).process(request);
        verify(request).complete();
    }

    @Test
    void manuallyReprocessCollectionRequestAndComplete() {
        prepareRequest();

        processingService.process(1L, true);

        verify(processor).reprocess(request);
        verify(request).complete();
    }

    @Test
    void throwExceptionWhenCollectionRequestDoesNotExist() {
        when(collectionRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processingService.process(1L))
            .isInstanceOfSatisfying(CollectionException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(COLLECTION_REQUEST_NOT_FOUND));
        verifyNoInteractions(processorRegistry);
    }

    private void prepareRequest() {
        when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(request.getSource()).thenReturn(HealthDataSource.SAMSUNG_HEALTH);
        when(request.getDataType()).thenReturn(HealthDataType.STEPS);
        when(processorRegistry.getProcessor(HealthDataSource.SAMSUNG_HEALTH, HealthDataType.STEPS)).thenReturn(processor);
    }
}
