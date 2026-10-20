package pl.krysinski.carrental.reservation.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.fleet.FleetInventory;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
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

    private final ReservationRepository reservations;
    private final FleetInventory fleet;
    private final ReservationMapper mapper;
    private final Clock clock;

    @Transactional
    public Reservation reserve(CreateReservationRequest request) {
        CarType carType = request.carType();
        RentalPeriod period = requestedPeriod(carType, request.start(), request.days());
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
        if (reservations.deleteReservation(id) == 0) {
            throw new ReservationNotFoundException(id);
        }
    }

    public Reservation find(UUID id) {
        return reservations.findById(id)
                .map(entity -> mapper.toReservation(entity, clock.getZone()))
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Availability availability(CarType carType, LocalDateTime start, int days) {
        RentalPeriod period = requestedPeriod(carType, start, days);
        return new Availability(carType, period, freeCars(fleet.capacityOf(carType), carType, period));
    }

    private RentalPeriod requestedPeriod(CarType carType, LocalDateTime start, int days) {
        if (carType == null) {
            throw new InvalidReservationException("Car type is required");//todo validation
        }
        RentalPeriod period = RentalPeriod.of(start, days, clock.getZone());
        if (period.start().toInstant().isBefore(clock.instant())) {
            throw new InvalidReservationException("Reservation cannot start in the past");//todo validation
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
