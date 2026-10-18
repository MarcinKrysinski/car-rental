package pl.krysinski.carrental.reservation.repository;

import org.springframework.stereotype.Repository;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public List<Reservation> findByCarType(CarType carType) {
        return reservations.values().stream()
                .filter(reservation -> reservation.carType() == carType)
                .toList();
    }

    @Override
    public boolean delete(UUID id) {
        return reservations.remove(id) != null;
    }
}
