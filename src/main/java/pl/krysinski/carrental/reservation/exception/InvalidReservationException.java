package pl.krysinski.carrental.reservation.exception;

public class InvalidReservationException extends RuntimeException {

    public InvalidReservationException(String message) {
        super(message);
    }
}
