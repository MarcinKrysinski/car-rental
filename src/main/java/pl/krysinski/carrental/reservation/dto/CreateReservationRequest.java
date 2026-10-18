package pl.krysinski.carrental.reservation.dto;

import jakarta.validation.constraints.NotNull;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull CarType carType,
        @NotNull LocalDateTime start,
        @NotNull Integer days,
        String customerId) {
}
