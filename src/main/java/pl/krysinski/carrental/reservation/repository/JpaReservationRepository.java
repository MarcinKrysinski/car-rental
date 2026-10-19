package pl.krysinski.carrental.reservation.repository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
@AllArgsConstructor
public class JpaReservationRepository implements ReservationRepository {

    private final SpringDataReservationRepository jpa;
    private final Clock clock;

    @Override
    public void save(Reservation reservation) {
        jpa.save(toReservationEntity(reservation));
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return jpa.findById(id).map(this::toReservation);
    }

    @Override
    public List<Reservation> findByCarType(CarType carType) {
        return jpa.findByCarType(carType).stream()
                .map(this::toReservation)
                .toList();
    }

    @Override
    public boolean delete(UUID id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    private ReservationEntity toReservationEntity(Reservation reservation) {
        return new ReservationEntity(
                reservation.id(),
                reservation.carType(),
                reservation.period().start().toInstant(),
                reservation.period().end().toInstant(),
                reservation.customerId());
    }

    private Reservation toReservation(ReservationEntity entity) {
        ZoneId zone = clock.getZone();
        RentalPeriod period = new RentalPeriod(entity.getStartAt().atZone(zone), entity.getEndAt().atZone(zone));
        return new Reservation(entity.getId(), entity.getCarType(), period, entity.getCustomerId());
    }
}
