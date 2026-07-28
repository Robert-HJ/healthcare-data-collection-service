package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import tools.jackson.databind.JsonNode;

public interface SourceValidator {

    HealthDataSource source();

    void validate(JsonNode payload);
}
