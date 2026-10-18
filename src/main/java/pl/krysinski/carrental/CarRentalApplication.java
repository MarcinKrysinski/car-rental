package pl.krysinski.carrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
public class CarRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarRentalApplication.class, args);
    }

    // The rental office's time zone: requested start times are read in it.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Warsaw"));
    }
}
