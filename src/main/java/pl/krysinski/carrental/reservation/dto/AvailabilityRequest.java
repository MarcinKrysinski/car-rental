package pl.krysinski.carrental.reservation.dto;

import org.springframework.format.annotation.DateTimeFormat;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.LocalDateTime;

public record AvailabilityRequest(
        CarType carType,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        Integer days) implements RentalRequest {
}
