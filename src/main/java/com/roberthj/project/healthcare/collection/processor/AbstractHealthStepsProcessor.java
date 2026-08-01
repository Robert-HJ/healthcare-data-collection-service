package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.aggregation.HealthStepDailyAggregationService;
import com.roberthj.project.healthcare.collection.entity.HealthDataCollectionRequestEntity;
import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataJdbcRepository;
import com.roberthj.project.healthcare.collection.repository.HealthStepDataUpsertRow;
import com.roberthj.project.healthcare.member.entity.MemberEntity;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractHealthStepsProcessor implements HealthDataProcessor {

    private final HealthStepDataJdbcRepository stepDataRepository;
    private final HealthStepDailyAggregationService aggregationService;

    protected AbstractHealthStepsProcessor(HealthStepDataJdbcRepository stepDataRepository, HealthStepDailyAggregationService aggregationService) {
        this.stepDataRepository = stepDataRepository;
        this.aggregationService = aggregationService;
    }

    @Override
    public final HealthDataFormat format() {
        return new HealthDataFormat(source(), HealthDataType.STEPS);
    }

    @Override
    public final void process(HealthDataCollectionRequestEntity request) {
        List<HealthStepDataUpsertRow> rows = createRows(request);
        stepDataRepository.upsertAll(rows);
        aggregate(request.getMember(), rows);
    }

    @Override
    public final void reprocess(HealthDataCollectionRequestEntity request) {
        List<HealthStepDataUpsertRow> rows = createRows(request);
        stepDataRepository.upsertAllForManualRetry(rows);
        aggregate(request.getMember(), rows);
    }

    protected abstract HealthDataSource source();

    protected abstract NormalizedStepData normalizeEntry(JsonNode entry);

    private List<HealthStepDataUpsertRow> createRows(HealthDataCollectionRequestEntity request) {
        Map<StepDataIdentity, NormalizedStepData> normalizedData = normalize(request.getPayload());
        MemberEntity member = request.getMember();
        Long requestId = request.getId();

        return normalizedData.values().stream()
            .map(data -> new HealthStepDataUpsertRow(member.getId(), requestId, source(), data.startedAt(),
                data.endedAt(), data.steps(), data.distance(), data.calories()))
            .toList();
    }

    private void aggregate(MemberEntity member, List<HealthStepDataUpsertRow> rows) {
        aggregationService.recalculate(member, source(), rows.stream().map(HealthStepDataUpsertRow::startedAt).toList());
    }

    private Map<StepDataIdentity, NormalizedStepData> normalize(JsonNode payload) {
        JsonNode entries = payload.path("data").path("entries");
        Map<StepDataIdentity, NormalizedStepData> normalizedData = new LinkedHashMap<>(entries.size());

        for (JsonNode entry : entries) {
            NormalizedStepData data = normalizeEntry(entry);
            normalizedData.put(new StepDataIdentity(data.startedAt(), data.endedAt()), data);
        }

        return normalizedData;
    }

    protected record NormalizedStepData(Instant startedAt, Instant endedAt, BigDecimal steps, BigDecimal distance, BigDecimal calories) {
    }

    private record StepDataIdentity(Instant startedAt, Instant endedAt) {
    }
}
