package pl.krysinski.carrental;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Freezes "now" in the application, so tests can book fixed dates that never slip into the past.
@TestConfiguration
public class FixedClockConfig {

    public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    public static final LocalDateTime NOW = LocalDateTime.of(2027, 6, 1, 8, 0);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW.atZone(WARSAW).toInstant(), WARSAW);
    }
}
