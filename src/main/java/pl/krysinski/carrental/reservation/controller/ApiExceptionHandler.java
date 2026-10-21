package pl.krysinski.carrental.reservation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;
import pl.krysinski.carrental.validation.RequestValidationException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidReservationException.class)
    public ProblemDetail invalidReservation(InvalidReservationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RequestValidationException.class)
    public ProblemDetail requestValidation(RequestValidationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setProperty("errors", e.getErrors().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getCode(), error.getDefaultMessage()))
                .toList());
        return problem;
    }

    record FieldViolation(String field, String code, String message) {
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail reservationNotFound(ReservationNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoAvailabilityException.class)
    public ProblemDetail noAvailability(NoAvailabilityException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> lockTimeout(PessimisticLockingFailureException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Too many reservations are being made right now, please try again");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}
