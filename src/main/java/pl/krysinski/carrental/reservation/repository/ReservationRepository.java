package pl.krysinski.carrental.reservation.repository;

import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    void save(Reservation reservation);

    Optional<Reservation> findById(UUID id);

    List<Reservation> findByCarType(CarType carType);

    // Returns false when there was no reservation with this id.
    boolean delete(UUID id);
}
