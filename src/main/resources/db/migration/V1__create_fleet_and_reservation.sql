CREATE TABLE fleet
(
    car_type VARCHAR(20) NOT NULL PRIMARY KEY,
    capacity INTEGER     NOT NULL
);

CREATE TABLE reservation
(
    id          UUID                     NOT NULL PRIMARY KEY,
    car_type    VARCHAR(20)              NOT NULL,
    start_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    customer_id VARCHAR(100)
);

CREATE INDEX idx_reservation_car_type_start_at ON reservation (car_type, start_at);

-- Initial fleet.
INSERT INTO fleet (car_type, capacity) VALUES ('SEDAN', 50);
INSERT INTO fleet (car_type, capacity) VALUES ('SUV', 60);
INSERT INTO fleet (car_type, capacity) VALUES ('VAN', 21);
