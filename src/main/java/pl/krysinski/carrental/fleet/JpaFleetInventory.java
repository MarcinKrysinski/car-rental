package pl.krysinski.carrental.fleet;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.EnumMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class JpaFleetInventory implements FleetInventory {

    private final FleetRepository fleetRepository;

    @Override
    public int capacityOf(CarType carType) {
        return fleetRepository.findById(carType)
                .map(FleetEntity::getCapacity)
                .orElse(0);
    }

    @Override
    public int lockCapacity(CarType carType) {
        return fleetRepository.findForUpdate(carType)
                .map(FleetEntity::getCapacity)
                .orElse(0);
    }

    @Override
    public Map<CarType, Integer> capacities() {
        Map<CarType, Integer> capacities = new EnumMap<>(CarType.class);
        for (CarType carType : CarType.values()) {
            capacities.put(carType, 0);
        }
        for (FleetEntity fleet : fleetRepository.findAll()) {
            capacities.put(fleet.getCarType(), fleet.getCapacity());
        }
        return capacities;
    }
}
