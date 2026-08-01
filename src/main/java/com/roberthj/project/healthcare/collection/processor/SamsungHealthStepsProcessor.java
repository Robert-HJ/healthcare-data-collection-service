package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataUpsertRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SamsungHealthStepsProcessor implements HealthDataProcessor {

    private static final ZoneId SOURCE_TIME_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("uuuu-MM-dd HH:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT);

    private final HealthStepDataJdbcRepository stepDataRepository;

    @Override
    public HealthDataFormat format() {
        return new HealthDataFormat(
            HealthDataSource.SAMSUNG_HEALTH,
            HealthDataType.STEPS
        );
    }

    @Override
    public void process(HealthDataCollectionRequestEntity request) {
        Map<StepDataIdentity, NormalizedStepData> normalizedData = normalize(request.getPayload());

        List<HealthStepDataUpsertRow> rows = normalizedData.values().stream()
            .map(data -> new HealthStepDataUpsertRow(
                request.getMember().getId(),
                request.getId(),
                HealthDataSource.SAMSUNG_HEALTH,
                data.startedAt(),
                data.endedAt(),
                data.steps(),
                data.distance(),
                data.calories()
            ))
            .toList();

        stepDataRepository.upsertAll(rows);
    }

    private Map<StepDataIdentity, NormalizedStepData> normalize(JsonNode payload) {
        JsonNode entries = payload.path("data").path("entries");
        Map<StepDataIdentity, NormalizedStepData> normalizedData = new LinkedHashMap<>(entries.size());

        for (JsonNode entry : entries) {
            JsonNode period = entry.path("period");
            NormalizedStepData data = new NormalizedStepData(
                toInstant(period.path("from").stringValue()),
                toInstant(period.path("to").stringValue()),
                entry.path("steps").decimalValue(),
                entry.path("distance").path("value").decimalValue(),
                entry.path("calories").path("value").decimalValue()
            );

            normalizedData.put(
                new StepDataIdentity(data.startedAt(), data.endedAt()),
                data
            );
        }

        return normalizedData;
    }

    private Instant toInstant(String value) {
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER)
            .atZone(SOURCE_TIME_ZONE)
            .toInstant();
    }

    private record StepDataIdentity(
        Instant startedAt,
        Instant endedAt
    ) {
    }

    private record NormalizedStepData(
        Instant startedAt,
        Instant endedAt,
        BigDecimal steps,
        BigDecimal distance,
        BigDecimal calories
    ) {
    }
}
