package pl.krysinski.carrental.reservation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.krysinski.carrental.fleet.FleetEntity;
import pl.krysinski.carrental.fleet.FleetRepository;
import pl.krysinski.carrental.reservation.dto.CreateReservationRequest;
import pl.krysinski.carrental.reservation.exception.NoAvailabilityException;
import pl.krysinski.carrental.reservation.exception.ReservationNotFoundException;
import pl.krysinski.carrental.reservation.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.krysinski.carrental.reservation.entity.CarType.SUV;

// Many customers try to book the same SUVs at the same moment, against the real H2 database.
// Not @Transactional: each reservation has to commit in its own transaction, as in production.
@SpringBootTest
class RentalServiceConcurrencyTest {

    private static final int CUSTOMERS = 20;
    // Far fewer cars than customers, whatever the seed data says.
    private static final int SUVS = 2;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private ReservationRepository reservations;

    private FleetEntity seededSuvs;

    @BeforeEach
    void setUp() {
        reservations.deleteAll();
        seededSuvs = fleetRepository.findById(SUV).orElseThrow();
        fleetRepository.save(new FleetEntity(SUV, SUVS));
    }

    // These writes commit, so put the seeded fleet back for the other test classes sharing the database.
    @AfterEach
    void restoreFleet() {
        reservations.deleteAll();
        fleetRepository.save(seededSuvs);
    }

    @RepeatedTest(5)
    void shouldBookExactlyAsManyCarsAsTheFleetHasWhenRequestsRace() throws Exception {
        LocalDateTime start = LocalDate.now().plusDays(30).atTime(10, 0);
        CountDownLatch allReady = new CountDownLatch(CUSTOMERS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS);
        try {
            List<Future<?>> requests = new ArrayList<>();
            for (int i = 0; i < CUSTOMERS; i++) {
                requests.add(pool.submit(() -> {
                    allReady.countDown();
                    go.await();
                    try {
                        rentalService.reserve(new CreateReservationRequest(SUV, start, 3, null));
                        booked.incrementAndGet();
                    } catch (NoAvailabilityException e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                }));
            }
            allReady.await();
            go.countDown();
            for (Future<?> request : requests) {
                request.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(booked).hasValue(SUVS);
        assertThat(rejected).hasValue(CUSTOMERS - SUVS);
        assertThat(reservations.count()).isEqualTo(SUVS);
    }

    @RepeatedTest(5)
    void shouldCancelReservationExactlyOnceWhenCancellationsRace() throws Exception {
        UUID id = rentalService.reserve(
                new CreateReservationRequest(SUV, LocalDate.now().plusDays(30).atTime(10, 0), 3, null)).id();
        CountDownLatch allReady = new CountDownLatch(CUSTOMERS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger notFound = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS);
        try {
            List<Future<?>> requests = new ArrayList<>();
            for (int i = 0; i < CUSTOMERS; i++) {
                requests.add(pool.submit(() -> {
                    allReady.countDown();
                    go.await();
                    try {
                        rentalService.cancel(id);
                        cancelled.incrementAndGet();
                    } catch (ReservationNotFoundException e) {
                        notFound.incrementAndGet();
                    }
                    return null;
                }));
            }
            allReady.await();
            go.countDown();
            // Any other exception (e.g. a stale-row error that would become HTTP 500) fails the test here.
            for (Future<?> request : requests) {
                request.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(cancelled).hasValue(1);
        assertThat(notFound).hasValue(CUSTOMERS - 1);
        assertThat(reservations.count()).isZero();
    }
}
