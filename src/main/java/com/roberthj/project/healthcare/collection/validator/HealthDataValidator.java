package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import tools.jackson.databind.JsonNode;

public interface HealthDataValidator {

    HealthDataFormat format();

    void validate(JsonNode payload);
}
