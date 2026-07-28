package com.roberthj.project.healthcare.collection.enums;

import java.util.Arrays;
import java.util.Optional;

public enum HealthDataType {

    STEPS("steps");

    private final String externalValue;

    HealthDataType(String externalValue) {
        this.externalValue = externalValue;
    }

    public static Optional<HealthDataType> findByExternalValue(String externalValue) {
        return Arrays.stream(values())
            .filter(type -> type.externalValue.equals(externalValue))
            .findFirst();
    }
}
