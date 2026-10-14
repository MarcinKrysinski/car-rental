package pl.krysinski.carrental.fleet;

import pl.krysinski.carrental.reservation.entity.CarType;

public interface FleetInventory {

    int capacityOf(CarType carType);
}
