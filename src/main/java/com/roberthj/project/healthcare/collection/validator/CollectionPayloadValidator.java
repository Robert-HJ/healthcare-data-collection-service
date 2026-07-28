package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
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

    private final Map<HealthDataSource, SourceValidator> validators;

    public CollectionPayloadValidator(List<SourceValidator> sourceValidators) {
        this.validators = sourceValidators.stream()
            .collect(Collectors.toUnmodifiableMap(
                SourceValidator::source,
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

    public void validateEntries(HealthDataSource source, JsonNode payload) {
        SourceValidator validator = validators.get(source);
        if (validator == null) {
            throw new IllegalStateException("등록된 Source 검증기가 없습니다: " + source);
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
