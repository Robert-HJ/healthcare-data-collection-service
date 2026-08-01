package com.roberthj.project.healthcare.collection.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class HealthStepAggregationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(HealthStepAggregationConfig.class);

    @Test
    void bindAggregationTimeZones() {
        contextRunner
            .withPropertyValues(
                "healthcare.collection.steps.aggregation.time-zones[0]=Asia/Seoul",
                "healthcare.collection.steps.aggregation.time-zones[1]=UTC",
                "healthcare.collection.steps.aggregation.default-time-zone=Asia/Seoul"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();

                HealthStepAggregationProperties properties = context.getBean(
                    HealthStepAggregationProperties.class);
                assertThat(properties.timeZones()).containsExactly(
                    ZoneId.of("Asia/Seoul"),
                    ZoneId.of("UTC")
                );
                assertThat(properties.defaultTimeZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
            });
    }

    @Test
    void failWhenDefaultTimeZoneIsNotSupported() {
        contextRunner
            .withPropertyValues(
                "healthcare.collection.steps.aggregation.time-zones[0]=UTC",
                "healthcare.collection.steps.aggregation.default-time-zone=Asia/Seoul"
            )
            .run(context -> assertThat(context).hasFailed());
    }
}
