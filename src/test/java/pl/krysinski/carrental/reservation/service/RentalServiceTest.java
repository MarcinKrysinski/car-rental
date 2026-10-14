package pl.krysinski.carrental.reservation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;
import pl.krysinski.carrental.reservation.repository.InMemoryReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.krysinski.carrental.reservation.entity.CarType.SEDAN;
import static pl.krysinski.carrental.reservation.entity.CarType.SUV;
import static pl.krysinski.carrental.reservation.entity.CarType.VAN;

class RentalServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime NOW = LocalDateTime.of(2027, 6, 1, 8, 0);
    private static final Map<CarType, Integer> FLEET = Map.of(SEDAN, 2, SUV, 2, VAN, 1);

    private final RentalService service = new RentalService(
            new InMemoryReservationRepository(),
            FLEET::get,
            Clock.fixed(NOW.atZone(WARSAW).toInstant(), WARSAW));

    @ParameterizedTest
    @EnumSource(CarType.class)
    void shouldReserveCarOfRequestedTypeForRequestedDateTimeAndDays(CarType carType) {
        Reservation reservation = service.reserve(carType, june(10), 3, "customer-1");

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
            service.reserve(carType, june(10), 3, null);
        }

        assertThatThrownBy(() -> service.reserve(carType, june(11), 1, null))
                .isInstanceOf(NoAvailabilityException.class);
    }

    @Test
    void shouldKeepSeparatePoolForEachCarType() {
        service.reserve(VAN, june(10), 3, null);

        assertThatThrownBy(() -> service.reserve(VAN, june(10), 3, null))
                .isInstanceOf(NoAvailabilityException.class);
        assertThat(service.reserve(SUV, june(10), 3, null)).isNotNull();
        assertThat(service.reserve(SEDAN, june(10), 3, null)).isNotNull();
    }

    @Test
    void shouldAllowNextReservationToStartExactlyWhenPreviousOneEnds() {
        service.reserve(VAN, june(10), 2, null);

        Reservation next = service.reserve(VAN, june(12), 2, null);

        assertThat(next.period().start().toLocalDateTime()).isEqualTo(june(12));
    }

    @Test
    void shouldAcceptReservationWhenOverlappingReservationsAreNeverActiveTogether() {
        // Two sedans. Bookings for days 2-4 and 6-8 both overlap a request for days 3-7, so counting
        // overlaps would call the fleet full. They are never active together, so one sedan can
        // serve both and the other is free for the new request.
        service.reserve(SEDAN, june(2), 2, null);
        service.reserve(SEDAN, june(6), 2, null);

        assertThat(service.reserve(SEDAN, june(3), 4, null)).isNotNull();
        // Now both sedans are out on day 3.
        assertThatThrownBy(() -> service.reserve(SEDAN, june(3), 1, null))
                .isInstanceOf(NoAvailabilityException.class);
    }

    @Test
    void shouldFreeCarWhenReservationIsCancelled() {
        Reservation reservation = service.reserve(VAN, june(10), 3, null);

        service.cancel(reservation.id());

        assertThat(service.reserve(VAN, june(10), 3, null)).isNotNull();
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
        assertThat(service.availability(SUV, june(10), 3).freeCars()).isEqualTo(2);

        service.reserve(SUV, june(10), 3, null);
        assertThat(service.availability(SUV, june(11), 1).freeCars()).isEqualTo(1);
        assertThat(service.availability(SUV, june(11), 1).available()).isTrue();

        service.reserve(SUV, june(10), 3, null);
        assertThat(service.availability(SUV, june(11), 1).freeCars()).isZero();
        assertThat(service.availability(SUV, june(11), 1).available()).isFalse();
        assertThat(service.availability(SUV, june(13), 1).freeCars()).isEqualTo(2);
    }

    @Test
    void shouldRejectReservationStartingInThePast() {
        assertThatThrownBy(() -> service.reserve(SUV, NOW.minusMinutes(1), 3, null))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("past");
    }

    @Test
    void shouldAcceptReservationStartingRightNow() {
        assertThat(service.reserve(SUV, NOW, 1, null)).isNotNull();
    }

    @Test
    void shouldRejectReservationShorterThanOneDay() {
        assertThatThrownBy(() -> service.reserve(SUV, june(10), 0, null))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void shouldRejectReservationWithoutCarTypeOrStart() {
        assertThatThrownBy(() -> service.reserve(null, june(10), 3, null))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(SUV, null, 3, null))
                .isInstanceOf(InvalidReservationException.class);
    }

    // 10:00 on the given day of June 2027.
    private static LocalDateTime june(int day) {
        return LocalDateTime.of(2027, 6, day, 10, 0);
    }
}
