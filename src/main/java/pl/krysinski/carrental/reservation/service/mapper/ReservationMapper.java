package pl.krysinski.carrental.reservation.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.krysinski.carrental.reservation.dto.ReservationResponse;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "start", source = "period.start")
    @Mapping(target = "end", source = "period.end")
    ReservationResponse toResponse(Reservation reservation);

}