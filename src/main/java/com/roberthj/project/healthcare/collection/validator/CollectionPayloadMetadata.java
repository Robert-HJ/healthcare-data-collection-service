package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;

public record CollectionPayloadMetadata(
    String recordKey,
    HealthDataType dataType,
    HealthDataSource source
) {
}
