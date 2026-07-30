package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.exception.CollectionException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static com.roberthj.project.healthcare.collection.enums.HealthDataSource.SAMSUNG_HEALTH;
import static com.roberthj.project.healthcare.collection.enums.HealthDataType.STEPS;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.INVALID_PAYLOAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectionPayloadValidatorTest {

    private static final String FIXTURE_PATH =
        "/fixtures/collection/samsung-health-valid.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CollectionPayloadValidator validator = new CollectionPayloadValidator(
        List.of(new SamsungHealthStepsValidator(), new HealthKitStepsValidator())
    );

    @Test
    void returnMetadataFromValidPayload() throws JacksonException {
        ObjectNode payload = loadPayload();

        CollectionPayloadMetadata metadata = validator.validateMetadata(payload);

        assertThat(metadata.recordKey()).isEqualTo("test-record-key");
        assertThat(metadata.dataType()).isEqualTo(STEPS);
        assertThat(metadata.source()).isEqualTo(SAMSUNG_HEALTH);
    }

    @Test
    void rejectUnsupportedSource() throws JacksonException {
        ObjectNode payload = loadPayload();
        ((ObjectNode) payload.path("data").path("source"))
            .put("name", "UnknownHealth");

        assertThatThrownBy(() -> validator.validateMetadata(payload))
            .isInstanceOfSatisfying(CollectionException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(INVALID_PAYLOAD)
            );
    }

    @Test
    void validateEntriesWithMatchingValidator() throws JacksonException {
        ObjectNode payload = loadPayload();
        CollectionPayloadMetadata metadata = validator.validateMetadata(payload);

        assertThatCode(() -> validator.validateEntries(metadata.format(), payload))
            .doesNotThrowAnyException();
    }

    private ObjectNode loadPayload() throws JacksonException {
        InputStream inputStream = Objects.requireNonNull(
            getClass().getResourceAsStream(FIXTURE_PATH)
        );
        return (ObjectNode) objectMapper.readTree(inputStream);
    }
}
