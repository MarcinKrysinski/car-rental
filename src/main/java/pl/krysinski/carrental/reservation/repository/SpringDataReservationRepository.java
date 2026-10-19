package pl.krysinski.carrental.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.List;
import java.util.UUID;

public interface SpringDataReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    List<ReservationEntity> findByCarType(CarType carType);
}
