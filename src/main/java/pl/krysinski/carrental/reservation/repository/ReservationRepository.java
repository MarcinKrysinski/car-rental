package pl.krysinski.carrental.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    Optional<ReservationEntity> findByIdAndDeletedFalse(UUID id);

    @Query("""
            select r from ReservationEntity r
            where r.carType = :carType and r.startAt < :end and r.endAt > :start
              and r.deleted = false
            """)
    List<ReservationEntity> findOverlapping(@Param("carType") CarType carType,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ReservationEntity r set r.deleted = true where r.id = :id and r.deleted = false")
    int delete(@Param("id") UUID id);
}
