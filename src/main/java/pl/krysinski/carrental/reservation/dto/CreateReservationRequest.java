package pl.krysinski.carrental.reservation.dto;

import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        CarType carType,
        LocalDateTime start,
        Integer days,
        String customerId) implements RentalRequest {
}
