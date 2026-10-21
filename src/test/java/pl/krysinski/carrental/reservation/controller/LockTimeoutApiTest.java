package pl.krysinski.carrental.reservation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.krysinski.carrental.FixedClockConfig;
import pl.krysinski.carrental.fleet.repository.FleetRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.krysinski.carrental.reservation.entity.CarType.VAN;

// A reservation that cannot get its turn on the fleet lock must answer 503 with Retry-After, not 500.
// Own database with a short LOCK_TIMEOUT, so the test waits a fraction of a second instead of 10.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:locktimeout;LOCK_TIMEOUT=200")
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
class LockTimeoutApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldAnswerServiceUnavailableWhenFleetStaysLockedLongerThanLockTimeout() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService otherCustomer = Executors.newSingleThreadExecutor();
        try {
            // Another reservation holds the van lock and does not finish in time.
            Future<?> holder = otherCustomer.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(transaction -> {
                        fleetRepository.findForUpdate(VAN);
                        locked.countDown();
                        awaitQuietly(release);
                    }));
            locked.await();

            mvc.perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"carType": "VAN", "start": "2027-06-10T10:00:00", "days": 2}
                                    """))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            otherCustomer.shutdownNow();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
