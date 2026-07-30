package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.INVALID_PAYLOAD;
import static com.roberthj.project.healthcare.collection.validator.ValidationUtils.invalid;
import static com.roberthj.project.healthcare.collection.validator.ValidationUtils.requireObject;
import static com.roberthj.project.healthcare.collection.validator.ValidationUtils.requireText;

@Component
public class HealthKitStepsValidator implements HealthDataValidator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssxx")
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public HealthDataFormat format() {
        return new HealthDataFormat(
            HealthDataSource.HEALTH_KIT,
            HealthDataType.STEPS
        );
    }

    @Override
    public void validate(JsonNode payload) {
        JsonNode entries = payload.path("data").path("entries");
        if (!entries.isArray() || entries.size() == 0) {
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
        OffsetDateTime from = parseDateTime(
            requireText(period, "from", entryPath + ".period.from"),
            entryPath + ".period.from"
        );
        OffsetDateTime to = parseDateTime(
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
        String fieldPath = entryPath + ".steps";
        String steps = requireText(entry, "steps", fieldPath);
        try {
            new BigDecimal(steps);
        } catch (NumberFormatException exception) {
            throw new CollectionException(
                INVALID_PAYLOAD,
                fieldPath + "는 숫자로 변환할 수 있는 문자열이어야 합니다.",
                exception
            );
        }
    }

    private OffsetDateTime parseDateTime(String value, String fieldPath) {
        try {
            return OffsetDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new CollectionException(
                INVALID_PAYLOAD,
                fieldPath + "의 시간 형식이 올바르지 않습니다.",
                exception
            );
        }
    }
}
