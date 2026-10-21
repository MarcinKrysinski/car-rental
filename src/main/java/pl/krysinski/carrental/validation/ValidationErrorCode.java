package pl.krysinski.carrental.validation;

import java.util.stream.Stream;

public enum ValidationErrorCode {

    REQUIRED("%1$s is required"),
    TOO_LONG("%1$s can be at most %2$d characters"),
    TOO_SMALL("%1$s must be at least %2$d"),
    TOO_LARGE("%1$s can be at most %2$d");

    private final String template;

    ValidationErrorCode(String template) {
        this.template = template;
    }

    public String message(String field, Object... args) {
        Object[] formatArgs = Stream.concat(Stream.of(field), Stream.of(args)).toArray();
        return template.formatted(formatArgs);
    }
}
