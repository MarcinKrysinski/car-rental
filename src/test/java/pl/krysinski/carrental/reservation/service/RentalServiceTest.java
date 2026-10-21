package pl.krysinski.carrental.reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.FixedClockConfig;
import pl.krysinski.carrental.fleet.FleetEntity;
import pl.krysinski.carrental.fleet.FleetRepository;
import pl.krysinski.carrental.reservation.dto.AvailabilityRequest;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.krysinski.carrental.FixedClockConfig.NOW;
import static pl.krysinski.carrental.reservation.entity.CarType.SEDAN;
import static pl.krysinski.carrental.reservation.entity.CarType.SUV;
import static pl.krysinski.carrental.reservation.entity.CarType.VAN;

// Against the real H2 database with "now" frozen at FixedClockConfig.NOW; every test is rolled back afterwards.
@SpringBootTest
@Import(FixedClockConfig.class)
@Transactional
class RentalServiceTest {

    private static final Map<CarType, Integer> FLEET = Map.of(SEDAN, 2, SUV, 2, VAN, 1);

    @Autowired
    private RentalService service;

    @Autowired
    private FleetRepository fleetRepository;

    @BeforeEach
    void setUpFleet() {
        FLEET.forEach((carType, capacity) -> fleetRepository.save(new FleetEntity(carType, capacity)));
    }

    @ParameterizedTest
    @EnumSource(CarType.class)
    void shouldReserveCarOfRequestedTypeForRequestedDateTimeAndDays(CarType carType) {
        Reservation reservation = reserve(carType, june(10), 3, "customer-1");

        assertThat(reservation.carType()).isEqualTo(carType);
        assertThat(reservation.period().start().toLocalDateTime()).isEqualTo(june(10));
        assertThat(reservation.period().end().toLocalDateTime()).isEqualTo(june(13));
        assertThat(reservation.customerId()).isEqualTo("customer-1");
        assertThat(service.find(reservation.id())).isEqualTo(reservation);
    }

    @ParameterizedTest
    @EnumSource(CarType.class)
    void shouldRejectReservationWhenAllCarsOfTypeAreBookedForOverlappingPeriod(CarType carType) {
        for (int i = 0; i < FLEET.get(carType); i++) {
            reserve(carType, june(10), 3, null);
        }

        assertThatThrownBy(() -> reserve(carType, june(11), 1, null))
                .isInstanceOf(NoAvailabilityException.class);
    }

    @Test
    void shouldKeepSeparatePoolForEachCarType() {
        reserve(VAN, june(10), 3, null);

        assertThatThrownBy(() -> reserve(VAN, june(10), 3, null))
                .isInstanceOf(NoAvailabilityException.class);
        assertThat(reserve(SUV, june(10), 3, null)).isNotNull();
        assertThat(reserve(SEDAN, june(10), 3, null)).isNotNull();
    }

    @Test
    void shouldAllowNextReservationToStartExactlyWhenPreviousOneEnds() {
        reserve(VAN, june(10), 2, null);

        Reservation next = reserve(VAN, june(12), 2, null);

        assertThat(next.period().start().toLocalDateTime()).isEqualTo(june(12));
    }

    @Test
    void shouldAcceptReservationWhenOverlappingReservationsAreNeverActiveTogether() {
        // Two sedans. Bookings for days 2-4 and 6-8 both overlap a request for days 3-7, so counting
        // overlaps would call the fleet full. They are never active together, so one sedan can
        // serve both and the other is free for the new request.
        reserve(SEDAN, june(2), 2, null);
        reserve(SEDAN, june(6), 2, null);

        assertThat(reserve(SEDAN, june(3), 4, null)).isNotNull();
        // Now both sedans are out on day 3.
        assertThatThrownBy(() -> reserve(SEDAN, june(3), 1, null))
                .isInstanceOf(NoAvailabilityException.class);
    }

    @Test
    void shouldFreeCarWhenReservationIsCancelled() {
        Reservation reservation = reserve(VAN, june(10), 3, null);

        service.cancel(reservation.id());

        assertThat(reserve(VAN, june(10), 3, null)).isNotNull();
        assertThatThrownBy(() -> service.find(reservation.id()))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void shouldFailToCancelReservationThatDoesNotExist() {
        assertThatThrownBy(() -> service.cancel(UUID.randomUUID()))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void shouldReportHowManyCarsAreStillFreeForPeriod() {
        assertThat(availability(june(10), 3).freeCars()).isEqualTo(2);

        reserve(SUV, june(10), 3, null);
        assertThat(availability(june(11), 1).freeCars()).isEqualTo(1);
        assertThat(availability(june(11), 1).available()).isTrue();

        reserve(SUV, june(10), 3, null);
        assertThat(availability(june(11), 1).freeCars()).isZero();
        assertThat(availability(june(11), 1).available()).isFalse();
        assertThat(availability(june(13), 1).freeCars()).isEqualTo(2);
    }

    @Test
    void shouldRejectReservationStartingInThePast() {
        assertThatThrownBy(() -> reserve(SUV, NOW.minusMinutes(1), 3, null))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("past");
    }

    @Test
    void shouldRejectAvailabilityCheckStartingInThePast() {
        assertThatThrownBy(() -> availability(NOW.minusMinutes(1), 3))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("past");
    }

    @Test
    void shouldAcceptReservationStartingRightNow() {
        assertThat(reserve(SUV, NOW, 1, null)).isNotNull();
    }

    @Test
    void shouldAcceptReservationStartingAtTheEndOfBookingHorizon() {
        assertThat(reserve(SUV, NOW.plusMonths(RentalService.MAX_MONTHS_AHEAD), 1, null)).isNotNull();
    }

    @Test
    void shouldRejectReservationStartingBeyondBookingHorizon() {
        assertThatThrownBy(() -> reserve(SUV, NOW.plusMonths(RentalService.MAX_MONTHS_AHEAD).plusMinutes(1), 1, null))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("months ahead");
        assertThatThrownBy(() -> reserve(SUV, LocalDateTime.MAX.withHour(10), 2, null))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("months ahead");
    }

    @Test
    void shouldRejectMissingValuesWithoutTouchingTheFleet() {
        // Callers other than the API skip its validators; this must still be a bad request, not a 409 or an NPE.
        assertThatThrownBy(() -> reserve(null, june(10), 3, null))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("Car type");
        assertThatThrownBy(() -> service.reserve(new CreateReservationRequest(SUV, june(10), null, null)))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("days");
        assertThatThrownBy(() -> service.availability(new AvailabilityRequest(null, june(10), 3)))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("Car type");
        assertThatThrownBy(() -> reserve(SUV, null, 3, null))
                .isInstanceOf(InvalidReservationException.class);
    }

    private Availability availability(LocalDateTime start, int days) {
        return service.availability(new AvailabilityRequest(SUV, start, days));
    }

    private Reservation reserve(CarType carType, LocalDateTime start, int days, String customerId) {
        return service.reserve(new CreateReservationRequest(carType, start, days, customerId));
    }

    // 10:00 on the given day of June 2027.
    private static LocalDateTime june(int day) {
        return LocalDateTime.of(2027, 6, day, 10, 0);
    }
}
