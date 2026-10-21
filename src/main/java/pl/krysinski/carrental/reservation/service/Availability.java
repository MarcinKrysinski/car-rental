package pl.krysinski.carrental.reservation.service;

import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;

public record Availability(CarType carType, RentalPeriod period, int freeCars) {

    public boolean available() {
        return freeCars > 0;
    }
}
