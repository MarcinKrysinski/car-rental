package pl.krysinski.carrental.reservation.validator;

import org.junit.jupiter.api.Test;
import pl.krysinski.carrental.reservation.dto.AvailabilityRequest;
import pl.krysinski.carrental.validation.RequestValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.krysinski.carrental.reservation.entity.CarType.SUV;

// Availability shares its checks with reservations (see CreateReservationRequestValidatorTest); this only proves they apply.
class AvailabilityRequestValidatorTest {

    private final AvailabilityRequestValidator validator = new AvailabilityRequestValidator();

    @Test
    void shouldAcceptCompleteRequest() {
        assertThatCode(() -> validator.validate(new AvailabilityRequest(SUV, LocalDateTime.of(2027, 6, 10, 10, 0), 2)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingValues() {
        assertThat(validator.validateObject(new AvailabilityRequest(null, null, null)).getFieldErrors())
                .extracting(error -> error.getField() + ":" + error.getCode())
                .containsExactly("carType:REQUIRED", "start:REQUIRED", "days:REQUIRED");
        assertThatThrownBy(() -> validator.validate(new AvailabilityRequest(SUV, null, 2)))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("start is required");
    }
}
