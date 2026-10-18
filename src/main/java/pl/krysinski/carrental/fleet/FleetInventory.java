package pl.krysinski.carrental.fleet;

import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.Map;

public interface FleetInventory {

    int capacityOf(CarType carType);

    Map<CarType, Integer> capacities();
}
