package pl.krysinski.carrental.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    @Query("""
            select r from ReservationEntity r
            where r.carType = :carType and r.startAt < :end and r.endAt > :start
            """)
    List<ReservationEntity> findOverlapping(@Param("carType") CarType carType,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ReservationEntity r where r.id = :id")
    int deleteReservation(@Param("id") UUID id);
}
