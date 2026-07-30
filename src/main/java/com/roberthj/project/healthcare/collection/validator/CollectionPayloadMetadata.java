package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;

public record CollectionPayloadMetadata(
    String recordKey,
    HealthDataType dataType,
    HealthDataSource source
) {

    public HealthDataFormat format() {
        return new HealthDataFormat(source, dataType);
    }
}
