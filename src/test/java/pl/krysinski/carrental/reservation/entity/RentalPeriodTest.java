package pl.krysinski.carrental.reservation.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalPeriodTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Test
    void shouldEndRequestedNumberOfDaysAfterStart() {
        RentalPeriod period = RentalPeriod.of(LocalDateTime.of(2027, 6, 10, 10, 0), 3, WARSAW);

        assertThat(period.start().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 6, 10, 10, 0));
        assertThat(period.end().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 6, 13, 10, 0));
    }

    @Test
    void shouldOverlapWhenPeriodsShareSomeTime() {
        RentalPeriod first = RentalPeriod.of(LocalDateTime.of(2027, 6, 10, 10, 0), 3, WARSAW);
        RentalPeriod second = RentalPeriod.of(LocalDateTime.of(2027, 6, 12, 10, 0), 3, WARSAW);

        assertThat(first.overlaps(second)).isTrue();
        assertThat(second.overlaps(first)).isTrue();
    }

    @Test
    void shouldNotOverlapWhenOnePeriodStartsExactlyWhenTheOtherEnds() {
        RentalPeriod first = RentalPeriod.of(LocalDateTime.of(2027, 6, 10, 10, 0), 2, WARSAW);
        RentalPeriod second = RentalPeriod.of(LocalDateTime.of(2027, 6, 12, 10, 0), 2, WARSAW);

        assertThat(first.overlaps(second)).isFalse();
        assertThat(second.overlaps(first)).isFalse();
    }

    @Test
    void shouldReturnCarAtSameWallClockTimeWhenClocksGoForward() {
        // summer time on 2027-03-28, so that day has only 23 hours.
        RentalPeriod period = RentalPeriod.of(LocalDateTime.of(2027, 3, 27, 10, 0), 2, WARSAW);

        assertThat(period.end().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 3, 29, 10, 0));
        assertThat(Duration.between(period.start(), period.end())).isEqualTo(Duration.ofHours(47));
    }

    @Test
    void shouldReturnCarAtSameWallClockTimeWhenClocksGoBack() {
        // winter time on 2027-10-31, so that day has 25 hours.
        RentalPeriod period = RentalPeriod.of(LocalDateTime.of(2027, 10, 30, 10, 0), 2, WARSAW);

        assertThat(period.end().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 11, 1, 10, 0));
        assertThat(Duration.between(period.start(), period.end())).isEqualTo(Duration.ofHours(49));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -30})
    void shouldRejectRentalShorterThanOneDay(int days) {
        assertThatThrownBy(() -> RentalPeriod.of(LocalDateTime.of(2027, 6, 10, 10, 0), days, WARSAW))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("at least one day");
    }

    @Test
    void shouldRejectMissingStart() {
        assertThatThrownBy(() -> RentalPeriod.of(null, 3, WARSAW))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void shouldRejectPeriodThatDoesNotEndAfterItStarts() {
        var start = LocalDateTime.of(2027, 6, 10, 10, 0).atZone(WARSAW);

        assertThatThrownBy(() -> new RentalPeriod(start, start))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> new RentalPeriod(start, start.minusDays(1)))
                .isInstanceOf(InvalidReservationException.class);
    }
}
