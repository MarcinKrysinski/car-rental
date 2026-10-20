package pl.krysinski.carrental.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.repository.ReservationEntity;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull CarType carType,
        @NotNull LocalDateTime start,
        @NotNull Integer days,
        @Size(max = ReservationEntity.CUSTOMER_ID_LENGTH) String customerId) {
}
