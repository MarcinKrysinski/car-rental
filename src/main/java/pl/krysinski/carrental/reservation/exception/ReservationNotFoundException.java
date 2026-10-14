package pl.krysinski.carrental.reservation.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID id) {
        super("Reservation " + id + " does not exist");
    }
}
