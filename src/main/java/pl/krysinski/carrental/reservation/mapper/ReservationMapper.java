package pl.krysinski.carrental.reservation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.krysinski.carrental.reservation.dto.ReservationResponse;
import pl.krysinski.carrental.reservation.entity.Reservation;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "start", source = "period.start")
    @Mapping(target = "end", source = "period.end")
    ReservationResponse toResponse(Reservation reservation);

    default LocalDateTime toLocalDateTime(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDateTime();
    }
}