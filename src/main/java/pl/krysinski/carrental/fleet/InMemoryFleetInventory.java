package pl.krysinski.carrental.fleet;

import org.springframework.stereotype.Component;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.EnumMap;
import java.util.Map;

@Component
public class InMemoryFleetInventory implements FleetInventory {

    //todo implement the functionality for adding to the fleet
    private static final Map<CarType, Integer> CAPACITIES = Map.of(
            CarType.SEDAN, 3,
            CarType.SUV, 2,
            CarType.VAN, 1);

    @Override
    public int capacityOf(CarType carType) {
        return CAPACITIES.get(carType);
    }

    @Override
    public Map<CarType, Integer> capacities() {
        return new EnumMap<>(CAPACITIES);
    }
}
