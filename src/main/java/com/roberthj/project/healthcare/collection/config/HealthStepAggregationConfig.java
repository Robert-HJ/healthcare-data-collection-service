package com.roberthj.project.healthcare.collection.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HealthStepAggregationProperties.class)
public class HealthStepAggregationConfig {
}
