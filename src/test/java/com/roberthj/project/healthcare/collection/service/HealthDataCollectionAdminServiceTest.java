package com.roberthj.project.healthcare.collection.service;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.repository.HealthDataCollectionRequestRepository;
import com.roberthj.project.healthcare.collection.response.HealthDataCollectionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.util.Optional;

import static com.roberthj.project.healthcare.collection.enums.CollectionRequestStatus.FAILED;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.COLLECTION_REQUEST_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDataCollectionAdminServiceTest {

    @Mock
    private HealthDataCollectionRequestRepository collectionRequestRepository;

    @Mock
    private HealthDataCollectionReprocessingService reprocessingService;

    @Mock
    private HealthDataCollectionRequestStateService requestStateService;

    @Mock
    private HealthDataCollectionRequestEntity request;

    @InjectMocks
    private HealthDataCollectionAdminService adminService;

    @Test
    void returnCollectionRequestStatus() {
        when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(request.getId()).thenReturn(1L);
        when(request.getStatus()).thenReturn(FAILED);

        HealthDataCollectionResponse response = adminService.getStatus(1L);

        assertThat(response.requestId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(FAILED);
    }

    @Test
    void requestManualReprocessing() {
        when(collectionRequestRepository.existsById(1L)).thenReturn(true);
        when(requestStateService.claimForManualReprocessing(1L)).thenReturn(true);

        adminService.reprocess(1L);

        verify(reprocessingService).reprocess(1L);
    }

    @Test
    void doNotSubmitManualReprocessingWhenRequestIsStillProcessing() {
        when(collectionRequestRepository.existsById(1L)).thenReturn(true);
        when(requestStateService.claimForManualReprocessing(1L)).thenReturn(false);

        adminService.reprocess(1L);

        verify(reprocessingService, never()).reprocess(1L);
    }

    @Test
    void recordFailureWhenAsyncTaskSubmissionIsRejected() {
        TaskRejectedException rejectedException = new TaskRejectedException("executor rejected task");
        when(collectionRequestRepository.existsById(1L)).thenReturn(true);
        when(requestStateService.claimForManualReprocessing(1L)).thenReturn(true);
        doThrow(rejectedException).when(reprocessingService).reprocess(1L);

        assertThatThrownBy(() -> adminService.reprocess(1L)).isSameAs(rejectedException);
        verify(requestStateService).fail(1L, rejectedException);
    }

    @Test
    void rejectMissingCollectionRequest() {
        when(collectionRequestRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.reprocess(1L))
            .isInstanceOfSatisfying(CollectionException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(COLLECTION_REQUEST_NOT_FOUND));
        verify(reprocessingService, never()).reprocess(1L);
    }
}
