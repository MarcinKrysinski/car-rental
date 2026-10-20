package pl.krysinski.carrental.reservation.entity;

import pl.krysinski.carrental.reservation.exception.InvalidReservationException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public record RentalPeriod(ZonedDateTime start, ZonedDateTime end) {

    public static final int MAX_DAYS = 90;

    public RentalPeriod {
        if (start == null || end == null) {
            throw new InvalidReservationException("Rental period needs both a start and an end");
        }
        if (!end.isAfter(start)) {
            throw new InvalidReservationException("Rental period must end after it starts");
        }
    }

    public static RentalPeriod of(LocalDateTime start, int days, ZoneId zone) {
        if (start == null) {
            throw new InvalidReservationException("Start date and time are required");
        }
        if (days < 1) {
            throw new InvalidReservationException("Rental must last at least one day, got " + days);
        }
        if (days > MAX_DAYS) {
            throw new InvalidReservationException("Rental can last at most " + MAX_DAYS + " days, got " + days);
        }
        ZonedDateTime zonedStart = start.atZone(zone);
        return new RentalPeriod(zonedStart, zonedStart.plusDays(days));
    }

    public boolean overlaps(RentalPeriod other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
