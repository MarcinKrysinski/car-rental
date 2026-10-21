package pl.krysinski.carrental.reservation.dto;

import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.LocalDateTime;

public interface RentalRequest {

    CarType carType();

    LocalDateTime start();

    Integer days();
}
