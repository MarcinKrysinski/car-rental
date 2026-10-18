package pl.krysinski.carrental.reservation.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.krysinski.carrental.reservation.dto.AvailabilityResponse;
import pl.krysinski.carrental.reservation.service.Availability;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface AvailabilityMapper {

    @Mapping(target = "start", source = "period.start")
    @Mapping(target = "end", source = "period.end")
    @Mapping(target = "available", expression = "java(availability.available())")
    AvailabilityResponse toResponse(Availability availability);

    default LocalDateTime toLocalDateTime(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDateTime();
    }
}
