package com.roberthj.project.healthcare.collection.response;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequest;
import com.roberthj.project.healthcare.collection.enums.CollectionRequestStatus;

public record HealthDataCollectionResponse(
    Long requestId,
    CollectionRequestStatus status
) {

    public static HealthDataCollectionResponse from(HealthDataCollectionRequest request) {
        return new HealthDataCollectionResponse(
            request.getId(),
            request.getStatus()
        );
    }
}
