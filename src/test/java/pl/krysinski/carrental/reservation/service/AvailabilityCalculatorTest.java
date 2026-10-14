package pl.krysinski.carrental.reservation.service;

import org.junit.jupiter.api.Test;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityCalculatorTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Test
    void shouldReportNoConcurrentBookingsWhenNothingIsBooked() {
        assertThat(AvailabilityCalculator.calculatePeakBookingCount(List.of(), days(2, 6))).isZero();
    }

    @Test
    void shouldNotAddUpBookingsThatOverlapTheWindowButNeverEachOther() {
        // Both bookings overlap days 2-6, but one car can serve all three: 1-3, then 5-7.
        List<RentalPeriod> booked = List.of(days(1, 3), days(5, 7));

        assertThat(AvailabilityCalculator.calculatePeakBookingCount(booked, days(2, 6))).isEqualTo(1);
    }

    @Test
    void shouldCountBookingsThatAreActiveAtTheSameTime() {
        List<RentalPeriod> booked = List.of(days(1, 4), days(3, 7), days(3, 5));

        assertThat(AvailabilityCalculator.calculatePeakBookingCount(booked, days(2, 6))).isEqualTo(3);
    }

    @Test
    void shouldNotCountBackToBackBookingsAsConcurrent() {
        List<RentalPeriod> booked = List.of(days(1, 3), days(3, 5), days(5, 7));

        assertThat(AvailabilityCalculator.calculatePeakBookingCount(booked, days(1, 7))).isEqualTo(1);
    }

    @Test
    void shouldIgnoreBookingsOutsideTheWindow() {
        List<RentalPeriod> booked = List.of(days(1, 3), days(1, 3), days(8, 10));

        assertThat(AvailabilityCalculator.calculatePeakBookingCount(booked, days(3, 8))).isZero();
    }

    @Test
    void shouldCountBookingThatSpansTheWholeWindow() {
        List<RentalPeriod> booked = List.of(days(1, 20), days(4, 5));

        assertThat(AvailabilityCalculator.calculatePeakBookingCount(booked, days(3, 8))).isEqualTo(2);
    }

    /** From 10:00 on day {@code from} to 10:00 on day {@code to} of June 2027. */
    private static RentalPeriod days(int from, int to) {
        return RentalPeriod.of(LocalDateTime.of(2027, 6, from, 10, 0), to - from, WARSAW);
    }
}
