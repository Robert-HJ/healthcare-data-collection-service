package com.roberthj.project.healthcare.collection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;
import java.util.List;

@ConfigurationProperties(prefix = "healthcare.collection.steps.aggregation")
public record HealthStepAggregationProperties(List<ZoneId> timeZones, ZoneId defaultTimeZone) {

    public HealthStepAggregationProperties {
        if (timeZones == null || timeZones.isEmpty()) {
            throw new IllegalArgumentException("지원 타임존은 하나 이상이어야 합니다.");
        }
        if (defaultTimeZone == null) {
            throw new IllegalArgumentException("기본 타임존은 필수입니다.");
        }

        timeZones = List.copyOf(timeZones);
        if (!timeZones.contains(defaultTimeZone)) {
            throw new IllegalArgumentException("기본 타임존은 지원 타임존에 포함되어야 합니다.");
        }
    }
}
