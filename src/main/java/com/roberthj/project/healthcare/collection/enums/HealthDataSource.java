package com.roberthj.project.healthcare.collection.enums;

import java.util.Arrays;
import java.util.Optional;

public enum HealthDataSource {

    SAMSUNG_HEALTH("SamsungHealth"),
    HEALTH_KIT("Health Kit");

    private final String externalName;

    HealthDataSource(String externalName) {
        this.externalName = externalName;
    }

    public static Optional<HealthDataSource> findByExternalName(String externalName) {
        return Arrays.stream(values())
            .filter(source -> source.externalName.equals(externalName))
            .findFirst();
    }
}
