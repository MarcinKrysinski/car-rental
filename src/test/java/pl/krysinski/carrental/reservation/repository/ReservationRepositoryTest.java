package pl.krysinski.carrental.reservation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.mapper.ReservationMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Against the real H2 database; every test is rolled back afterwards.
@SpringBootTest
@Transactional
class ReservationRepositoryTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    private ReservationRepository repository;

    @Autowired
    private ReservationMapper mapper;

    @Test
    void shouldReadBackSavedReservationUnchanged() {
        Reservation reservation = save(CarType.SUV, LocalDateTime.of(2027, 6, 10, 10, 0), 3);

        assertThat(repository.findById(reservation.id()).map(entity -> mapper.toReservation(entity, WARSAW)))
                .contains(reservation);
    }

    @Test
    void shouldKeepWallClockTimeAcrossDaylightSavingChange() {
        // Europe/Warsaw switches to summer time on 2027-03-28.
        Reservation reservation = save(CarType.VAN, LocalDateTime.of(2027, 3, 27, 10, 0), 2);

        Reservation loaded = mapper.toReservation(repository.findById(reservation.id()).orElseThrow(), WARSAW);
        assertThat(loaded.period().start().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 3, 27, 10, 0));
        assertThat(loaded.period().end().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 3, 29, 10, 0));
    }

    @Test
    void shouldFindOnlyOverlappingReservationsOfRequestedType() {
        Reservation overlapping = save(CarType.SUV, LocalDateTime.of(2027, 6, 9, 10, 0), 3);
        save(CarType.VAN, LocalDateTime.of(2027, 6, 10, 10, 0), 3);
        save(CarType.SUV, LocalDateTime.of(2027, 1, 10, 10, 0), 3);
        save(CarType.SUV, LocalDateTime.of(2027, 7, 10, 10, 0), 3);

        assertThat(findOverlappingSuvs(LocalDateTime.of(2027, 6, 10, 10, 0), 3))
                .extracting(ReservationEntity::getId)
                .containsExactly(overlapping.id());
    }

    @Test
    void shouldNotTreatBackToBackReservationsAsOverlapping() {
        save(CarType.SUV, LocalDateTime.of(2027, 6, 8, 10, 0), 2);
        save(CarType.SUV, LocalDateTime.of(2027, 6, 13, 10, 0), 2);

        assertThat(findOverlappingSuvs(LocalDateTime.of(2027, 6, 10, 10, 0), 3)).isEmpty();
    }

    @Test
    void shouldReportHowManyReservationsWereDeleted() {
        Reservation reservation = save(CarType.SEDAN, LocalDateTime.of(2027, 6, 10, 10, 0), 1);

        assertThat(repository.deleteReservation(reservation.id())).isEqualTo(1);
        assertThat(repository.findById(reservation.id())).isEmpty();
        assertThat(repository.deleteReservation(reservation.id())).isZero();
    }

    private Reservation save(CarType carType, LocalDateTime start, int days) {
        Reservation reservation =
                new Reservation(UUID.randomUUID(), carType, RentalPeriod.of(start, days, WARSAW), "customer-1");
        repository.save(mapper.toEntity(reservation));
        return reservation;
    }

    private List<ReservationEntity> findOverlappingSuvs(LocalDateTime start, int days) {
        RentalPeriod period = RentalPeriod.of(start, days, WARSAW);
        return repository.findOverlapping(CarType.SUV, period.start().toInstant(), period.end().toInstant());
    }
}
