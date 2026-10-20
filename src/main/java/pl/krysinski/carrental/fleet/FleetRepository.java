package pl.krysinski.carrental.fleet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.Optional;

public interface FleetRepository extends JpaRepository<FleetEntity, CarType> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FleetEntity f where f.carType = :carType")
    Optional<FleetEntity> findForUpdate(@Param("carType") CarType carType);
}
