package pl.krysinski.carrental.reservation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Against the real H2 database; every test is rolled back afterwards.
@SpringBootTest
@Transactional
class JpaReservationRepositoryTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    private ReservationRepository repository;

    @Test
    void shouldUseDatabaseRepositoryInTheApplication() {
        assertThat(repository).isInstanceOf(JpaReservationRepository.class);
    }

    @Test
    void shouldReadBackSavedReservationUnchanged() {
        Reservation reservation = reservation(CarType.SUV, LocalDateTime.of(2027, 6, 10, 10, 0), 3);

        repository.save(reservation);

        assertThat(repository.findById(reservation.id())).contains(reservation);
    }

    @Test
    void shouldKeepWallClockTimeAcrossDaylightSavingChange() {
        // Europe/Warsaw switches to summer time on 2027-03-28.
        Reservation reservation = reservation(CarType.VAN, LocalDateTime.of(2027, 3, 27, 10, 0), 2);

        repository.save(reservation);

        Reservation loaded = repository.findById(reservation.id()).orElseThrow();
        assertThat(loaded.period().start().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 3, 27, 10, 0));
        assertThat(loaded.period().end().toLocalDateTime()).isEqualTo(LocalDateTime.of(2027, 3, 29, 10, 0));
    }

    @Test
    void shouldFindOnlyReservationsOfRequestedType() {
        Reservation suv = reservation(CarType.SUV, LocalDateTime.of(2027, 6, 10, 10, 0), 3);
        Reservation van = reservation(CarType.VAN, LocalDateTime.of(2027, 6, 10, 10, 0), 3);
        repository.save(suv);
        repository.save(van);

        assertThat(repository.findByCarType(CarType.SUV)).containsExactly(suv);
    }

    @Test
    void shouldReportWhetherReservationWasDeleted() {
        Reservation reservation = reservation(CarType.SEDAN, LocalDateTime.of(2027, 6, 10, 10, 0), 1);
        repository.save(reservation);

        assertThat(repository.delete(reservation.id())).isTrue();
        assertThat(repository.findById(reservation.id())).isEmpty();
        assertThat(repository.delete(reservation.id())).isFalse();
    }

    private static Reservation reservation(CarType carType, LocalDateTime start, int days) {
        return new Reservation(UUID.randomUUID(), carType, RentalPeriod.of(start, days, WARSAW), "customer-1");
    }
}
