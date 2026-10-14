package pl.krysinski.carrental.reservation.exception;

import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;

public class NoAvailabilityException extends RuntimeException {

    public NoAvailabilityException(CarType carType, RentalPeriod period) {
        super("No %s available from %s to %s".formatted(
                carType, period.start().toLocalDateTime(), period.end().toLocalDateTime()));
    }
}
