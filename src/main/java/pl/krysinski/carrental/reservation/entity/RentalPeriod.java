package pl.krysinski.carrental.reservation.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public record RentalPeriod(ZonedDateTime start, ZonedDateTime end) {

    public static RentalPeriod of(LocalDateTime start, int days, ZoneId zone) {
        ZonedDateTime zonedStart = start.atZone(zone);
        return new RentalPeriod(zonedStart, zonedStart.plusDays(days));
    }

    public boolean overlaps(RentalPeriod other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
