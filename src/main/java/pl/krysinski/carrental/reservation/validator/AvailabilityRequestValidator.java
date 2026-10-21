package pl.krysinski.carrental.reservation.validator;

import org.springframework.stereotype.Component;
import pl.krysinski.carrental.reservation.dto.AvailabilityRequest;

@Component
public class AvailabilityRequestValidator extends RentalRequestValidator<AvailabilityRequest> {

    public AvailabilityRequestValidator() {
        super(AvailabilityRequest.class);
    }
}
