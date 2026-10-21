package pl.krysinski.carrental.reservation.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.krysinski.carrental.reservation.dto.AvailabilityRequest;
import pl.krysinski.carrental.reservation.dto.AvailabilityResponse;
import pl.krysinski.carrental.reservation.mapper.AvailabilityMapper;
import pl.krysinski.carrental.reservation.service.RentalService;
import pl.krysinski.carrental.reservation.validator.AvailabilityRequestValidator;

@RestController
@RequestMapping("/api/v1/availability")
@AllArgsConstructor
public class AvailabilityController {

    private final RentalService rentalService;
    private final AvailabilityMapper availabilityMapper;
    private final AvailabilityRequestValidator availabilityRequestValidator;

    @GetMapping
    public ResponseEntity<AvailabilityResponse> check(@ModelAttribute AvailabilityRequest request) {
        availabilityRequestValidator.validate(request);
        return ResponseEntity.ok(availabilityMapper.toResponse(rentalService.availability(request)));
    }
}
