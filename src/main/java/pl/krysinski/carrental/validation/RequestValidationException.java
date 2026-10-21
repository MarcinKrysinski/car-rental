package pl.krysinski.carrental.validation;

import lombok.Getter;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import java.util.stream.Collectors;

@Getter
public class RequestValidationException extends RuntimeException {

    private final Errors errors;

    public RequestValidationException(Errors errors) {
        super(errors.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; ")));
        this.errors = errors;
    }
}
