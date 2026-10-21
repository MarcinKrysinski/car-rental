package pl.krysinski.carrental.reservation.validator;

import org.springframework.validation.Errors;
import pl.krysinski.carrental.reservation.dto.RentalRequest;
import pl.krysinski.carrental.validation.GenericValidator;

import static pl.krysinski.carrental.validation.ValidationErrorCode.REQUIRED;
import static pl.krysinski.carrental.validation.ValidationErrorCode.TOO_LARGE;
import static pl.krysinski.carrental.validation.ValidationErrorCode.TOO_SMALL;

public abstract class RentalRequestValidator<T extends RentalRequest> extends GenericValidator<T> {

    public static final int MAX_DAYS = 90;

    protected RentalRequestValidator(Class<T> type) {
        super(type);
    }

    @Override
    protected void doValidate(T request, Errors errors) {
        if (request.carType() == null) {
            rejectWithError(errors, "carType", REQUIRED);
        }
        if (request.start() == null) {
            rejectWithError(errors, "start", REQUIRED);
        }
        Integer days = request.days();
        if (days == null) {
            rejectWithError(errors, "days", REQUIRED);
        } else if (days < 1) {
            rejectWithError(errors, "days", TOO_SMALL, 1);
        } else if (days > MAX_DAYS) {
            rejectWithError(errors, "days", TOO_LARGE, MAX_DAYS);
        }
    }
}
