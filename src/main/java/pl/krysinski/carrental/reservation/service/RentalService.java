package pl.krysinski.carrental.reservation.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.fleet.service.FleetInventory;
import pl.krysinski.carrental.reservation.dto.AvailabilityRequest;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.dto.RentalRequest;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;
import pl.krysinski.carrental.reservation.mapper.ReservationMapper;
import pl.krysinski.carrental.reservation.repository.ReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RentalService {

    public static final int MAX_MONTHS_AHEAD = 12;

    private final ReservationRepository reservations;
    private final FleetInventory fleet;
    private final ReservationMapper mapper;
    private final Clock clock;

    @Transactional
    public Reservation reserve(CreateReservationRequest request) {
        CarType carType = request.carType();
        RentalPeriod period = requestedPeriod(request);
        int capacity = fleet.lockCapacity(carType);
        if (freeCars(capacity, carType, period) < 1) {
            throw new NoAvailabilityException(carType, period);
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), carType, period, request.customerId());
        reservations.save(mapper.toEntity(reservation));
        return reservation;
    }

    @Transactional
    public void cancel(UUID id) {
        if (reservations.markCancelled(id) == 0) {
            throw new ReservationNotFoundException(id);
        }
    }

    public Reservation find(UUID id) {
        return reservations.findByIdAndDeletedFalse(id)
                .map(entity -> mapper.toReservation(entity, clock.getZone()))
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Availability availability(AvailabilityRequest request) {
        CarType carType = request.carType();
        RentalPeriod period = requestedPeriod(request);
        return new Availability(carType, period, freeCars(fleet.capacityOf(carType), carType, period));
    }


    private RentalPeriod requestedPeriod(RentalRequest request) {
        if (request.carType() == null) {
            throw new InvalidReservationException("Car type is required");
        }
        if (request.days() == null) {
            throw new InvalidReservationException("Rental length in days is required");
        }
        LocalDateTime start = request.start();
        if (start == null) {
            throw new InvalidReservationException("Start date and time are required");
        }
        if (start.isAfter(LocalDateTime.now(clock).plusMonths(MAX_MONTHS_AHEAD))) {
            throw new InvalidReservationException(
                    "Reservation can start at most " + MAX_MONTHS_AHEAD + " months ahead");
        }
        RentalPeriod period = RentalPeriod.of(start, request.days(), clock.getZone());
        if (period.start().toInstant().isBefore(clock.instant())) {
            throw new InvalidReservationException("Reservation cannot start in the past");
        }
        return period;
    }

    private int freeCars(int capacity, CarType carType, RentalPeriod period) {
        List<RentalPeriod> booked = reservations
                .findOverlapping(carType, period.start().toInstant(), period.end().toInstant()).stream()
                .map(entity -> mapper.toReservation(entity, clock.getZone()).period())
                .toList();
        return capacity - AvailabilityCalculator.calculatePeakBookingCount(booked, period);
    }
}
