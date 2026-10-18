package pl.krysinski.carrental.reservation.dto;

import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(UUID id, CarType carType, LocalDateTime start, LocalDateTime end, String customerId) {
}
