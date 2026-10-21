package pl.krysinski.carrental.fleet.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.krysinski.carrental.fleet.service.FleetInventory;
import pl.krysinski.carrental.reservation.entity.CarType;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fleet")
@AllArgsConstructor
public class FleetController {

    private final FleetInventory fleetInventory;

    @GetMapping
    public ResponseEntity<Map<CarType, Integer>> capacities() {
        return ResponseEntity.ok(fleetInventory.capacities());
    }

    @GetMapping("/{carType}")
    public ResponseEntity<Integer> capacity(@PathVariable CarType carType) {
        return ResponseEntity.ok(fleetInventory.capacityOf(carType));
    }
}
