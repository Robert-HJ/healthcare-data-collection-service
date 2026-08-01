package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.config.HealthDataCollectionWorkerProperties;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestClaimRepository;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionRequestStateServiceTest {

    @Mock
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Mock
    private HealthDataCollectionRequestClaimRepository claimRepository;

    @Mock
    private HealthDataCollectionRequestEntity request;

    private HealthDataCollectionRequestStateService requestStateService;

    @BeforeEach
    void setUp() {
        HealthDataCollectionWorkerProperties properties = new HealthDataCollectionWorkerProperties(4, 5, Duration.ofMinutes(5), Duration.ofMinutes(1));
        requestStateService = new HealthDataCollectionRequestStateService(collectionRequestRepository, claimRepository, properties);
    }

    @Test
    void updateClaimedRequestToProcessing() {
        when(claimRepository.findNextRequest(5, Duration.ofMinutes(5))).thenReturn(Optional.of(1L));

        requestStateService.claimNext();

        verify(claimRepository).updateProcessing(1L);
    }

    @Test
    void updateFailedRequestState() {
        RuntimeException processingException = new RuntimeException("processing failed");
        when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        requestStateService.fail(1L, processingException);

        verify(request).fail("processing failed");
    }
}
