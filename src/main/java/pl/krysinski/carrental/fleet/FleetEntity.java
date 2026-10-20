package pl.krysinski.carrental.fleet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.krysinski.carrental.reservation.entity.CarType;

@Entity
@Table(name = "fleet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FleetEntity {

    @Id
    @Enumerated(EnumType.STRING)
    private CarType carType;

    @Column(nullable = false)
    private int capacity;
}
