package pl.krysinski.carrental.reservation.service;

import pl.krysinski.carrental.fleet.FleetInventory;
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

public class RentalService {

    private final ReservationRepository reservations;
    private final FleetInventory fleet;
    private final Clock clock;

    // The clock's zone is the rental office's zone: requested start times are read in it.
    public RentalService(ReservationRepository reservations, FleetInventory fleet, Clock clock) {
        this.reservations = reservations;
        this.fleet = fleet;
        this.clock = clock;
    }

    public Reservation reserve(CarType carType, LocalDateTime start, int days, String customerId) {
        RentalPeriod period = requestedPeriod(carType, start, days);
        if (freeCars(carType, period) < 1) {
            throw new NoAvailabilityException(carType, period);
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), carType, period, customerId);
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
            throw new InvalidReservationException("Car type is required");
        }
        RentalPeriod period = RentalPeriod.of(start, days, clock.getZone());
        if (period.start().toInstant().isBefore(clock.instant())) {
            throw new InvalidReservationException("Reservation cannot start in the past");
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
