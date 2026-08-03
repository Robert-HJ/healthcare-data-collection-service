package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationService;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

@Component
public class HealthKitStepsProcessor extends AbstractHealthStepsProcessor {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssxx")
        .withResolverStyle(ResolverStyle.STRICT);

    public HealthKitStepsProcessor(HealthStepDataJdbcRepository stepDataRepository, HealthStepDailyAggregationService aggregationService) {
        super(stepDataRepository, aggregationService);
    }

    @Override
    protected HealthDataSource source() {
        return HealthDataSource.HEALTH_KIT;
    }

    @Override
    protected NormalizedStepData normalizeEntry(JsonNode entry) {
        JsonNode period = entry.path("period");
        return new NormalizedStepData(toInstant(period.path("from").stringValue()),
            toInstant(period.path("to").stringValue()), new BigDecimal(entry.path("steps").stringValue()),
            entry.path("distance").path("value").decimalValue(), entry.path("calories").path("value").decimalValue());
    }

    private Instant toInstant(String value) {
        // 1. HealthKit 시각에 포함된 UTC 오프셋을 보존하여 UTC 시각으로 변환
        return OffsetDateTime.parse(value, DATE_TIME_FORMATTER).toInstant();
    }
}
