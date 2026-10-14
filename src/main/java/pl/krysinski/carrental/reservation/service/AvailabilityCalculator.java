package pl.krysinski.carrental.reservation.service;

import lombok.NoArgsConstructor;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@NoArgsConstructor
public final class AvailabilityCalculator {

    private record BookingEvent(Instant at, int delta) {
    }

    public static int calculatePeakBookingCount(Collection<RentalPeriod> bookedPeriods, RentalPeriod window) {
        Instant windowStart = window.start().toInstant();
        Instant windowEnd = window.end().toInstant();
        List<BookingEvent> bookingEvents = new ArrayList<>();
        for (RentalPeriod period : bookedPeriods) {
            if (!period.overlaps(window)) {
                continue;
            }
            Instant start = period.start().toInstant();
            Instant end = period.end().toInstant();
            bookingEvents.add(new BookingEvent(start.isBefore(windowStart) ? windowStart : start, +1));
            bookingEvents.add(new BookingEvent(end.isAfter(windowEnd) ? windowEnd : end, -1));
        }
        // Ends sort before starts at same instant:  car returned at 10:00 is free at 10:00.
        bookingEvents.sort(Comparator.comparing(BookingEvent::at).thenComparingInt(BookingEvent::delta));

        int activeBookings = 0;
        int peak = 0;
        for (BookingEvent bookingEvent : bookingEvents) {
            activeBookings += bookingEvent.delta();
            peak = Math.max(peak, activeBookings);
        }
        return peak;
    }
}
