package pl.krysinski.carrental.reservation.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pl.krysinski.carrental.fleet.FleetInventory;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.exception.InvalidReservationException;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;
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
    private final Clock clock;

    public Reservation reserve(CreateReservationRequest request) {
        CarType carType = request.carType();
        RentalPeriod period = requestedPeriod(carType, request.start(), request.days());
        if (freeCars(carType, period) < 1) {
            throw new NoAvailabilityException(carType, period);
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), carType, period, request.customerId());
        reservations.save(reservation);
        return reservation;
    }

    public void cancel(UUID id) {
        if (!reservations.delete(id)) {
            throw new ReservationNotFoundException(id);
        }
    }

    public Reservation find(UUID id) {
        return reservations.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Availability availability(CarType carType, LocalDateTime start, int days) {
        RentalPeriod period = requestedPeriod(carType, start, days);
        return new Availability(carType, period, freeCars(carType, period));
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

    private int freeCars(CarType carType, RentalPeriod period) {
        List<RentalPeriod> booked = reservations.findByCarType(carType).stream()
                .map(Reservation::period)
                .toList();
        return fleet.capacityOf(carType) - AvailabilityCalculator.calculatePeakBookingCount(booked, period);
    }
}
