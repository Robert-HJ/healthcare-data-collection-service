package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.roberthj.project.healthcare.collection.validator.ValidationUtils.invalid;
import static com.roberthj.project.healthcare.collection.validator.ValidationUtils.requireObject;
import static com.roberthj.project.healthcare.collection.validator.ValidationUtils.requireText;

@Component
public class CollectionPayloadValidator {

    private final Map<HealthDataFormat, HealthDataValidator> validators;

    public CollectionPayloadValidator(List<HealthDataValidator> healthDataValidators) {
        this.validators = healthDataValidators.stream()
            .collect(Collectors.toUnmodifiableMap(
                HealthDataValidator::format,
                Function.identity()
            ));
    }

    public CollectionPayloadMetadata validateMetadata(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw invalid("요청 본문은 객체여야 합니다.");
        }

        String recordKey = requireText(payload, "recordkey", "recordkey");
        HealthDataType dataType = parseDataType(
            requireText(payload, "type", "type")
        );

        JsonNode data = requireObject(payload, "data", "data");
        JsonNode sourceNode = requireObject(data, "source", "data.source");
        HealthDataSource source = parseSource(
            requireText(sourceNode, "name", "data.source.name")
        );

        return new CollectionPayloadMetadata(recordKey, dataType, source);
    }

    public void validateEntries(HealthDataFormat format, JsonNode payload) {
        HealthDataValidator validator = validators.get(format);
        if (validator == null) {
            throw invalid("지원하지 않는 source와 type 조합입니다.");
        }

        validator.validate(payload);
    }

    private HealthDataType parseDataType(String externalValue) {
        return HealthDataType.findByExternalValue(externalValue)
            .orElseThrow(() -> invalid("지원하지 않는 type입니다."));
    }

    private HealthDataSource parseSource(String externalName) {
        return HealthDataSource.findByExternalName(externalName)
            .orElseThrow(() -> invalid("지원하지 않는 data.source.name입니다."));
    }
}
