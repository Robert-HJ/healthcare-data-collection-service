package com.roberthj.project.healthcare.collection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "healthcare.collection.worker")
public record HealthDataCollectionWorkerProperties(int concurrency, int maxRetryCount, Duration staleProcessingTimeout, Duration pollingInterval) {

    public HealthDataCollectionWorkerProperties {
        if (concurrency < 1) {
            throw new IllegalArgumentException("수집 처리 동시 실행 수는 1 이상이어야 합니다.");
        }
        if (maxRetryCount < 1) {
            throw new IllegalArgumentException("최대 재시도 횟수는 1 이상이어야 합니다.");
        }
        if (staleProcessingTimeout == null || staleProcessingTimeout.isNegative() || staleProcessingTimeout.isZero()) {
            throw new IllegalArgumentException("장기 처리 판단 시간은 0보다 커야 합니다.");
        }
        if (pollingInterval == null || pollingInterval.isNegative() || pollingInterval.isZero()) {
            throw new IllegalArgumentException("수집 요청 확인 주기는 0보다 커야 합니다.");
        }
    }
}
