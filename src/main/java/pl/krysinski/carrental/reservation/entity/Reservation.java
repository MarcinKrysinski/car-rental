package pl.krysinski.carrental.reservation.entity;

import java.util.Objects;
import java.util.UUID;

public record Reservation(UUID id, CarType carType, RentalPeriod period, String customerId) {

    public Reservation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(carType, "carType");
        Objects.requireNonNull(period, "period");
    }
}
