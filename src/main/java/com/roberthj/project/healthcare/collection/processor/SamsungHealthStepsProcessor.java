package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationService;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

@Component
public class SamsungHealthStepsProcessor extends AbstractHealthStepsProcessor {

    private static final ZoneId SOURCE_TIME_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("uuuu-MM-dd HH:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT);

    public SamsungHealthStepsProcessor(HealthStepDataJdbcRepository stepDataRepository, HealthStepDailyAggregationService aggregationService) {
        super(stepDataRepository, aggregationService);
    }

    @Override
    protected HealthDataSource source() {
        return HealthDataSource.SAMSUNG_HEALTH;
    }

    @Override
    protected NormalizedStepData normalizeEntry(JsonNode entry) {
        JsonNode period = entry.path("period");
        return new NormalizedStepData(toInstant(period.path("from").stringValue()),
            toInstant(period.path("to").stringValue()), entry.path("steps").decimalValue(),
            entry.path("distance").path("value").decimalValue(), entry.path("calories").path("value").decimalValue());
    }

    private Instant toInstant(String value) {
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER).atZone(SOURCE_TIME_ZONE).toInstant();
    }
}
