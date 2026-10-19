package pl.krysinski.carrental.reservation.controller;

import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.krysinski.carrental.reservation.dto.AvailabilityResponse;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.service.RentalService;
import pl.krysinski.carrental.reservation.mapper.AvailabilityMapper;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/availability")
@AllArgsConstructor
public class AvailabilityController {

    private final RentalService rentalService;
    private final AvailabilityMapper availabilityMapper;

    @GetMapping
    public ResponseEntity<AvailabilityResponse> check(
            @RequestParam CarType carType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam int days) {

        return ResponseEntity.ok(availabilityMapper.toResponse(rentalService.availability(carType, start, days)));
    }
}
