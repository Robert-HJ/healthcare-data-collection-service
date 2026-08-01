package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;

public interface HealthDataProcessor {

    HealthDataFormat format();

    void process(HealthDataCollectionRequestEntity request);
}
