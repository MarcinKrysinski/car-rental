package pl.krysinski.carrental.reservation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.krysinski.carrental.reservation.dto.ReservationResponse;
import pl.krysinski.carrental.reservation.entity.RentalPeriod;
import pl.krysinski.carrental.reservation.entity.Reservation;
import pl.krysinski.carrental.reservation.repository.ReservationEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "start", source = "period.start")
    @Mapping(target = "end", source = "period.end")
    ReservationResponse toResponse(Reservation reservation);

    @Mapping(target = "startAt", source = "period.start")
    @Mapping(target = "endAt", source = "period.end")
    @Mapping(target = "deleted", constant = "false")
    ReservationEntity toEntity(Reservation reservation);

    default Reservation toReservation(ReservationEntity entity, ZoneId zone) {
        RentalPeriod period = new RentalPeriod(entity.getStartAt().atZone(zone), entity.getEndAt().atZone(zone));
        return new Reservation(entity.getId(), entity.getCarType(), period, entity.getCustomerId());
    }

    default LocalDateTime toLocalDateTime(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDateTime();
    }

    default Instant toInstant(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }
}
