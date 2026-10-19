package pl.krysinski.carrental.reservation.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.dto.ReservationResponse;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.service.RentalService;
import pl.krysinski.carrental.reservation.mapper.ReservationMapper;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@AllArgsConstructor
public class ReservationController {

    private final RentalService rentalService;
    private final ReservationMapper reservationMapper;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reserve = rentalService.reserve(request);
        return new ResponseEntity<>(reservationMapper.toResponse(reserve), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> get(@PathVariable UUID id) {
        Reservation reservation = rentalService.find(id);
        return ResponseEntity.ok(reservationMapper.toResponse(reservation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        rentalService.cancel(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
