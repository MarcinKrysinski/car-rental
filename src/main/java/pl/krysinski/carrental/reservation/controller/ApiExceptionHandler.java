package pl.krysinski.carrental.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidReservationException.class)
    public ProblemDetail invalidReservation(InvalidReservationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail reservationNotFound(ReservationNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoAvailabilityException.class)
    public ProblemDetail noAvailability(NoAvailabilityException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
