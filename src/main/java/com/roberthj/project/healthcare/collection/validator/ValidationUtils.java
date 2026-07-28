package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.exception.CollectionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.INVALID_PAYLOAD;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ValidationUtils {

    static JsonNode requireObject(JsonNode parent, String fieldName, String fieldPath) {
        JsonNode value = parent.get(fieldName);
        if (value == null || !value.isObject()) {
            throw invalid(fieldPath + "는 객체여야 합니다.");
        }
        return value;
    }

    static String requireText(JsonNode parent, String fieldName, String fieldPath) {
        JsonNode value = parent.get(fieldName);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw invalid(fieldPath + "는 비어 있지 않은 문자열이어야 합니다.");
        }
        return value.stringValue();
    }

    static CollectionException invalid(String detailMessage) {
        return new CollectionException(INVALID_PAYLOAD, detailMessage);
    }
}
