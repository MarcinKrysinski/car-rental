package pl.krysinski.carrental.reservation.dto;

import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.LocalDateTime;

public record AvailabilityResponse(CarType carType, LocalDateTime start, LocalDateTime end, boolean available, int freeCars) {
}
