package pl.krysinski.carrental.reservation.entity;

import java.util.Objects;
import java.util.UUID;

// Books a car type, not a specific car. customerId is optional.
public record Reservation(UUID id, CarType carType, RentalPeriod period, String customerId) {

    public Reservation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(carType, "carType");
        Objects.requireNonNull(period, "period");
    }
}
