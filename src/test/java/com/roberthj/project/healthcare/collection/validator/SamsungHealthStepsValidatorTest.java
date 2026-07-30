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

class SamsungHealthStepsValidatorTest {

    private static final String FIXTURE_PATH =
        "/fixtures/collection/samsung-health-valid.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SamsungHealthStepsValidator validator = new SamsungHealthStepsValidator();

    @Test
    void acceptValidPayloadIncludingZeroDurationEntry() throws JacksonException {
        ObjectNode payload = loadPayload();

        assertThatCode(() -> validator.validate(payload))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectInvalidDateTime() throws JacksonException {
        ObjectNode payload = loadPayload();
        periodOfFirstEntry(payload).put("from", "2024-02-30 10:00:00");

        assertInvalidPayload(payload);
    }

    @Test
    void rejectStringSteps() throws JacksonException {
        ObjectNode payload = loadPayload();
        firstEntry(payload).put("steps", "120");

        assertInvalidPayload(payload);
    }

    @Test
    void rejectUnsupportedUnit() throws JacksonException {
        ObjectNode payload = loadPayload();
        ((ObjectNode) firstEntry(payload).get("distance")).put("unit", "m");

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
