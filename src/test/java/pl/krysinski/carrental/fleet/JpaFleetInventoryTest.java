package pl.krysinski.carrental.fleet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Against the real H2 database; every test is rolled back afterwards.
@SpringBootTest
@Transactional
class JpaFleetInventoryTest {

    @Autowired
    private FleetInventory fleetInventory;

    @Autowired
    private FleetRepository fleetRepository;

    @Test
    void shouldUseDatabaseFleetInTheApplication() {
        assertThat(fleetInventory).isInstanceOf(JpaFleetInventory.class);
    }

    @Test
    void shouldSeedEveryCarTypeAtStartup() {
        assertThat(fleetRepository.findAll()).hasSize(CarType.values().length);
    }

    @Test
    void shouldReadCapacitiesFromDatabase() {
        fleetRepository.save(new FleetEntity(CarType.SEDAN, 3));
        fleetRepository.save(new FleetEntity(CarType.SUV, 2));
        fleetRepository.save(new FleetEntity(CarType.VAN, 1));

        assertThat(fleetInventory.capacities())
                .containsExactly(Map.entry(CarType.SEDAN, 3), Map.entry(CarType.SUV, 2), Map.entry(CarType.VAN, 1));
        assertThat(fleetInventory.capacityOf(CarType.SUV)).isEqualTo(2);
        assertThat(fleetInventory.lockCapacity(CarType.SUV)).isEqualTo(2);
    }

    @Test
    void shouldTreatTypeWithoutRowAsHavingNoCars() {
        fleetRepository.deleteById(CarType.VAN);

        assertThat(fleetInventory.capacityOf(CarType.VAN)).isZero();
        assertThat(fleetInventory.capacities()).containsEntry(CarType.VAN, 0);
    }
}
