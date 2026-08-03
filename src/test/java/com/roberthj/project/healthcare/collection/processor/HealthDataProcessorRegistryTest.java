package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.HEALTH_KIT;
import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.SAMSUNG_HEALTH;
import static com.roberthj.project.healthcare.collection.enums.HealthDataType.STEPS;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.PROCESSOR_ALREADY_REGISTERED;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.PROCESSOR_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthDataProcessorRegistryTest {

    @Test
    void returnProcessorMatchingSourceAndDataType() {
        HealthDataProcessor samsungStepsProcessor = processor(new HealthDataFormat(SAMSUNG_HEALTH, STEPS));
        HealthDataProcessorRegistry registry = new HealthDataProcessorRegistry(List.of(samsungStepsProcessor));

        HealthDataProcessor result = registry.getProcessor(SAMSUNG_HEALTH, STEPS);

        assertThat(result).isSameAs(samsungStepsProcessor);
    }

    @Test
    void rejectUnsupportedSourceAndDataType() {
        HealthDataProcessorRegistry registry = new HealthDataProcessorRegistry(List.of(processor(new HealthDataFormat(SAMSUNG_HEALTH, STEPS))));

        assertThatThrownBy(() -> registry.getProcessor(HEALTH_KIT, STEPS))
            .isInstanceOfSatisfying(CollectionException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(PROCESSOR_NOT_FOUND));
    }

    @Test
    void rejectDuplicateProcessorFormat() {
        HealthDataFormat format = new HealthDataFormat(SAMSUNG_HEALTH, STEPS);

        assertThatThrownBy(() -> new HealthDataProcessorRegistry(List.of(processor(format), processor(format))))
            .isInstanceOfSatisfying(CollectionException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(PROCESSOR_ALREADY_REGISTERED));
    }

    private HealthDataProcessor processor(HealthDataFormat format) {
        return new HealthDataProcessor() {

            @Override
            public HealthDataFormat format() {
                return format;
            }

            @Override
            public void process(HealthDataCollectionRequestEntity request) {
            }
        };
    }
}
