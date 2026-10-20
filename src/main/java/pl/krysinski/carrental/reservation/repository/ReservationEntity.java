package pl.krysinski.carrental.reservation.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation", indexes = @Index(columnList = "carType, startAt"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReservationEntity {

    public static final int CUSTOMER_ID_LENGTH = 100;

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarType carType;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Column(length = CUSTOMER_ID_LENGTH)
    private String customerId;
}
