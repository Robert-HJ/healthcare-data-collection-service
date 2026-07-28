package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.INVALID_PAYLOAD;

@Component
public class SamsungHealthValidator implements SourceValidator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public HealthDataSource source() {
        return HealthDataSource.SAMSUNG_HEALTH;
    }

    @Override
    public void validate(JsonNode payload) {
        JsonNode entries = payload.path("data").path("entries");
        if (!entries.isArray() || entries.isEmpty()) {
            throw invalid("data.entries는 비어 있지 않은 배열이어야 합니다.");
        }

        for (int index = 0; index < entries.size(); index++) {
            validateEntry(entries.get(index), index);
        }
    }

    private void validateEntry(JsonNode entry, int index) {
        String entryPath = "data.entries[" + index + "]";
        if (!entry.isObject()) {
            throw invalid(entryPath + "는 객체여야 합니다.");
        }

        JsonNode period = requireObject(entry, "period", entryPath + ".period");
        LocalDateTime from = parseDateTime(
            requireText(period, "from", entryPath + ".period.from"),
            entryPath + ".period.from"
        );
        LocalDateTime to = parseDateTime(
            requireText(period, "to", entryPath + ".period.to"),
            entryPath + ".period.to"
        );
        if (from.isAfter(to)) {
            throw invalid(entryPath + ".period.from은 to보다 늦을 수 없습니다.");
        }

        validateMeasurement(entry, "distance", "km", entryPath);
        validateMeasurement(entry, "calories", "kcal", entryPath);
        validateSteps(entry, entryPath);
    }

    private void validateMeasurement(
        JsonNode entry,
        String fieldName,
        String expectedUnit,
        String entryPath
    ) {
        String fieldPath = entryPath + "." + fieldName;
        JsonNode measurement = requireObject(entry, fieldName, fieldPath);
        String unit = requireText(measurement, "unit", fieldPath + ".unit");
        if (!expectedUnit.equals(unit)) {
            throw invalid(fieldPath + ".unit은 " + expectedUnit + "이어야 합니다.");
        }

        JsonNode value = measurement.get("value");
        if (value == null || !value.isNumber()) {
            throw invalid(fieldPath + ".value는 숫자여야 합니다.");
        }
    }

    private void validateSteps(JsonNode entry, String entryPath) {
        JsonNode steps = entry.get("steps");
        if (steps == null || !steps.isIntegralNumber()) {
            throw invalid(entryPath + ".steps는 정수여야 합니다.");
        }
    }

    private JsonNode requireObject(JsonNode parent, String fieldName, String fieldPath) {
        JsonNode value = parent.get(fieldName);
        if (value == null || !value.isObject()) {
            throw invalid(fieldPath + "는 객체여야 합니다.");
        }
        return value;
    }

    private String requireText(JsonNode parent, String fieldName, String fieldPath) {
        JsonNode value = parent.get(fieldName);
        if (value == null || !value.isTextual() || value.stringValue().isBlank()) {
            throw invalid(fieldPath + "는 비어 있지 않은 문자열이어야 합니다.");
        }
        return value.stringValue();
    }

    private LocalDateTime parseDateTime(String value, String fieldPath) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new CollectionException(
                INVALID_PAYLOAD,
                fieldPath + "의 시간 형식이 올바르지 않습니다.",
                exception
            );
        }
    }

    private CollectionException invalid(String detailMessage) {
        return new CollectionException(INVALID_PAYLOAD, detailMessage);
    }
}
