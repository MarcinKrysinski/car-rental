package pl.krysinski.carrental.reservation.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.Errors;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.repository.ReservationEntity;
import pl.krysinski.carrental.validation.RequestValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.krysinski.carrental.reservation.entity.CarType.VAN;

// Input rules only; time-dependent rules (past start, booking horizon) are covered in RentalServiceTest.
class CreateReservationRequestValidatorTest {

    private static final LocalDateTime START = LocalDateTime.of(2027, 6, 10, 10, 0);

    private final CreateReservationRequestValidator validator = new CreateReservationRequestValidator();

    @Test
    void shouldAcceptCompleteRequest() {
        assertThatCode(() -> validator.validate(new CreateReservationRequest(VAN, START, 2, "customer-1")))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(new CreateReservationRequest(VAN, START, 2, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldLeaveTimeDependentRulesToTheService() {
        assertThat(errorsFor(new CreateReservationRequest(VAN, LocalDateTime.MIN, 2, null))).isEmpty();
        assertThat(errorsFor(new CreateReservationRequest(VAN, LocalDateTime.MAX, 2, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void shouldRejectRentalShorterThanOneDay(int days) {
        assertThat(errorsFor(new CreateReservationRequest(VAN, START, days, null)))
                .containsExactly("days:TOO_SMALL");
    }

    @ParameterizedTest
    @ValueSource(ints = {RentalRequestValidator.MAX_DAYS + 1, 5200, Integer.MAX_VALUE})
    void shouldRejectRentalLongerThanMaximum(int days) {
        assertThat(errorsFor(new CreateReservationRequest(VAN, START, days, null)))
                .containsExactly("days:TOO_LARGE");
        assertThatThrownBy(() -> validator.validate(new CreateReservationRequest(VAN, START, days, null)))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("days can be at most " + RentalRequestValidator.MAX_DAYS);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, RentalRequestValidator.MAX_DAYS})
    void shouldAcceptRentalLengthWithinLimits(int days) {
        assertThat(errorsFor(new CreateReservationRequest(VAN, START, days, null))).isEmpty();
    }

    @Test
    void shouldReportEveryMissingFieldAtOnce() {
        assertThat(errorsFor(new CreateReservationRequest(null, null, null, null)))
                .containsExactly("carType:REQUIRED", "start:REQUIRED", "days:REQUIRED");
    }

    @Test
    void shouldRejectTooLongCustomerId() {
        String customerId = "x".repeat(ReservationEntity.CUSTOMER_ID_LENGTH + 1);

        assertThat(errorsFor(new CreateReservationRequest(VAN, START, 2, customerId)))
                .containsExactly("customerId:TOO_LONG");
    }

    @Test
    void shouldThrowWithReadableMessages() {
        assertThatThrownBy(() -> validator.validate(new CreateReservationRequest(null, START, null, null)))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("carType is required; days is required");
    }

    private List<String> errorsFor(CreateReservationRequest request) {
        Errors errors = validator.validateObject(request);
        return errors.getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getCode())
                .toList();
    }
}
