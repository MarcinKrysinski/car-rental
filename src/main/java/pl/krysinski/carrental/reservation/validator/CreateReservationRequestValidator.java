package pl.krysinski.carrental.reservation.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.repository.ReservationEntity;

import static pl.krysinski.carrental.validation.ValidationErrorCode.TOO_LONG;

@Component
public class CreateReservationRequestValidator extends RentalRequestValidator<CreateReservationRequest> {

    public CreateReservationRequestValidator() {
        super(CreateReservationRequest.class);
    }

    @Override
    protected void doValidate(CreateReservationRequest request, Errors errors) {
        super.doValidate(request, errors);
        String customerId = request.customerId();
        if (customerId != null && customerId.length() > ReservationEntity.CUSTOMER_ID_LENGTH) {
            rejectWithError(errors, "customerId", TOO_LONG, ReservationEntity.CUSTOMER_ID_LENGTH);
        }
    }
}
