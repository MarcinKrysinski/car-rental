package pl.krysinski.carrental.validation;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public abstract class GenericValidator<T> implements Validator {

    private final Class<T> type;

    protected GenericValidator(Class<T> type) {
        this.type = type;
    }

    protected abstract void doValidate(T target, Errors errors);

    public void validate(T target) {
        Errors errors = validateObject(target);
        if (errors.hasErrors()) {
            throw new RequestValidationException(errors);
        }
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return type.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        doValidate(type.cast(target), errors);
    }

    protected void rejectWithError(Errors errors, String field, ValidationErrorCode code, Object... args) {
        errors.rejectValue(field, code.name(), args, code.message(field, args));
    }
}
