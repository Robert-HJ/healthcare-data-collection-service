package com.roberthj.project.healthcare.collection.validator;

import com.roberthj.project.healthcare.collection.exception.CollectionException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.util.Objects;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.INVALID_PAYLOAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthKitStepsValidatorTest {

    private static final String FIXTURE_PATH =
        "/fixtures/collection/health-kit-valid.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HealthKitStepsValidator validator = new HealthKitStepsValidator();

    @Test
    void acceptValidPayloadIncludingDecimalSteps() throws JacksonException {
        ObjectNode payload = loadPayload();

        assertThatCode(() -> validator.validate(payload))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectDateTimeWithoutOffset() throws JacksonException {
        ObjectNode payload = loadPayload();
        periodOfFirstEntry(payload).put("from", "2024-11-14T21:20:00");

        assertInvalidPayload(payload);
    }

    @Test
    void rejectNumericSteps() throws JacksonException {
        ObjectNode payload = loadPayload();
        firstEntry(payload).put("steps", 24);

        assertInvalidPayload(payload);
    }

    @Test
    void rejectNonNumericSteps() throws JacksonException {
        ObjectNode payload = loadPayload();
        firstEntry(payload).put("steps", "not-a-number");

        assertInvalidPayload(payload);
    }

    private ObjectNode loadPayload() throws JacksonException {
        InputStream inputStream = Objects.requireNonNull(
            getClass().getResourceAsStream(FIXTURE_PATH)
        );
        return (ObjectNode) objectMapper.readTree(inputStream);
    }

    private ObjectNode firstEntry(ObjectNode payload) {
        return (ObjectNode) payload.path("data").path("entries").get(0);
    }

    private ObjectNode periodOfFirstEntry(ObjectNode payload) {
        return (ObjectNode) firstEntry(payload).get("period");
    }

    private void assertInvalidPayload(ObjectNode payload) {
        assertThatThrownBy(() -> validator.validate(payload))
            .isInstanceOfSatisfying(CollectionException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(INVALID_PAYLOAD)
            );
    }
}
