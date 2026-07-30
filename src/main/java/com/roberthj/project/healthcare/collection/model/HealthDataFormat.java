package com.roberthj.project.healthcare.collection.model;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;

public record HealthDataFormat(
    HealthDataSource source,
    HealthDataType dataType
) {
}
