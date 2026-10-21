# Car Rental

A simulated car rental system. A customer reserves a car of a given type (Sedan, SUV or Van) from a
date and time for a number of days. Each type has a limited number of cars, and the system never
hands out more cars of a type than the fleet has, even when many requests arrive at once.

Java 21, Spring Boot 4, Spring Data JPA, Flyway, H2 (in memory), MapStruct, Lombok, Maven.

- [Running](#running)
- [Assumptions](#assumptions)
- [API](#api)
- [Errors](#errors)
- [Validation](#validation)
- [Concurrency: why a pessimistic lock](#concurrency-why-a-pessimistic-lock)
- [How availability is calculated](#how-availability-is-calculated)
- [Database and Flyway](#database-and-flyway)
- [Soft delete](#soft-delete)
- [Tests](#tests)
- [Limitations and trade-offs](#limitations-and-trade-offs)
- [Project layout](#project-layout)

## Running

Requires JDK 21. On Windows use `mvnw.cmd` instead of `./mvnw`.

```bash
./mvnw spring-boot:run
```

The app listens on `http://localhost:8080`:

- `http://localhost:8080/`: a small web panel for trying the API by hand (fleet, availability check,
  booking, cancelling).
- `http://localhost:8080/api/v1/...`: the REST API described below.

Run the tests:

```bash
./mvnw test
```

The database is in memory, so every restart starts from the initial fleet with no reservations.

## Assumptions

**Fleet**

- There are three car types: `SEDAN`, `SUV`, `VAN`. The list is fixed in code (`CarType`).
- Cars are not tracked one by one. A type is a pool of identical cars, and a reservation takes "one
  car of this type", not a specific vehicle.
- The initial fleet is 50 sedans, 60 SUVs and 21 vans. It lives in the `fleet` table and is loaded by
  a Flyway migration. The API cannot change it.

**Time**

- The rental office works in one time zone, `Europe/Warsaw` (the `Clock` bean in
  `CarRentalApplication`).
- The API takes and returns local date-times without an offset, e.g. `2026-12-01T10:00:00`. They are
  read as Warsaw time. In the database they are stored as instants (`TIMESTAMP WITH TIME ZONE`).
- A rental of `N` days ends at the same wall-clock time `N` calendar days later. Across a daylight
  saving change the rental is therefore 47 or 49 hours long, not 48. The customer returns the car at
  the time printed on the contract.
- The end of a rental is exclusive. A car returned at 10:00 can be rented again from 10:00.

**Rules for a reservation**

| Rule | Value | Checked in |
|---|---|---|
| Car type is required and must be a known type | `SEDAN`, `SUV`, `VAN` | validator / JSON parsing |
| Start is required | ISO-8601 local date-time | validator |
| Length in days | 1 to 90 (`RentalRequestValidator.MAX_DAYS`) | validator |
| Customer id | optional, at most 100 characters | validator |
| Start cannot be in the past | compared with the current time in Warsaw | `RentalService` |
| Booking horizon | start at most 12 months ahead (`RentalService.MAX_MONTHS_AHEAD`) | `RentalService` |
| A car of the type must be free for the whole period | otherwise `409 Conflict` | `RentalService`, under a lock |

**Customers**

- There are no user accounts and no authentication. `customerId` is a free-text label stored with
  the reservation and is not checked against anything.
- The reservation id is a random UUID. Knowing it is enough to read or cancel the reservation.

## API

All endpoints are under `/api/v1`. Bodies are JSON. Errors are returned as
[RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`).

| Method | Path | What it does | Success |
|---|---|---|---|
| `GET` | `/fleet` | Number of cars of every type | `200` |
| `GET` | `/fleet/{carType}` | Number of cars of one type | `200` |
| `GET` | `/availability?carType=&start=&days=` | How many cars of a type are free for a period | `200` |
| `POST` | `/reservations` | Book a car | `201` |
| `GET` | `/reservations/{id}` | Read a reservation | `200` |
| `DELETE` | `/reservations/{id}` | Cancel a reservation | `204` |

The examples below use bash (Linux, macOS, Git Bash or WSL). Responses were captured from a running
app (some error bodies are shortened to the relevant fields); ids will differ.

### Fleet

```bash
curl http://localhost:8080/api/v1/fleet
```

```json
{"SEDAN":50,"SUV":60,"VAN":21}
```

```bash
curl http://localhost:8080/api/v1/fleet/VAN
```

```json
21
```

### Check availability

```bash
curl "http://localhost:8080/api/v1/availability?carType=SUV&start=2026-12-01T10:00:00&days=3"
```

```json
{"carType":"SUV","start":"2026-12-01T10:00:00","end":"2026-12-04T10:00:00","available":true,"freeCars":60}
```

`freeCars` is the number of cars free for the **whole** period, i.e. the fleet size minus the busiest
moment within it. The check takes no lock and reserves nothing, so a car shown as free can be taken
by someone else before you book it. The booking itself checks again.

### Book a car

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{"carType":"SUV","start":"2026-12-01T10:00:00","days":3,"customerId":"customer-42"}'
```

`201 Created`:

```json
{"id":"cfe51e9b-63c3-474d-ac47-48e7669c4124","carType":"SUV","start":"2026-12-01T10:00:00","end":"2026-12-04T10:00:00","customerId":"customer-42"}
```

`customerId` can be left out. It is then `null` in the response.

### Read a reservation

```bash
curl http://localhost:8080/api/v1/reservations/cfe51e9b-63c3-474d-ac47-48e7669c4124
```

```json
{"id":"cfe51e9b-63c3-474d-ac47-48e7669c4124","carType":"SUV","start":"2026-12-01T10:00:00","end":"2026-12-04T10:00:00","customerId":"customer-42"}
```

### Cancel a reservation

```bash
curl -i -X DELETE http://localhost:8080/api/v1/reservations/cfe51e9b-63c3-474d-ac47-48e7669c4124
```

```
HTTP/1.1 204
```

After that, reading or cancelling the same id answers `404` (see [Soft delete](#soft-delete)).

### Full example: book, read, cancel

```bash
API=http://localhost:8080/api/v1

ID=$(curl -s -X POST $API/reservations \
  -H "Content-Type: application/json" \
  -d '{"carType":"VAN","start":"2026-12-01T10:00:00","days":2}' \
  | sed 's/.*"id":"\([^"]*\)".*/\1/')

curl $API/reservations/$ID              # 200
curl -i -X DELETE $API/reservations/$ID # 204
curl $API/reservations/$ID              # 404
```

## Errors

| Status | When | Body |
|---|---|---|
| `400` | A field is missing or out of range | `detail` plus an `errors` list, one entry per bad field |
| `400` | Start in the past, or more than 12 months ahead | `detail` only |
| `400` | Malformed JSON, unknown car type, id that is not a UUID | `detail` only, generic message |
| `404` | No reservation with this id, or it was cancelled | `detail` |
| `409` | No car of the requested type is free for the whole period | `detail` |
| `503` | The lock on the car type was not granted in time (see below) | `detail`, header `Retry-After: 1` |
| `500` | Anything unexpected. Details go to the log, not to the client | `detail: "Unexpected error"` |

Field errors list every problem at once:

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{"start":"2026-12-01T10:00:00","days":0}'
```

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "carType is required; days must be at least 1",
  "instance": "/api/v1/reservations",
  "errors": [
    {"field": "carType", "code": "REQUIRED", "message": "carType is required"},
    {"field": "days", "code": "TOO_SMALL", "message": "days must be at least 1"}
  ]
}
```

Error codes (`ValidationErrorCode`): `REQUIRED`, `TOO_SMALL`, `TOO_LARGE`, `TOO_LONG`.

Rental too long:

```bash
curl "http://localhost:8080/api/v1/availability?carType=SEDAN&start=2026-12-01T10:00:00&days=5200"
```

```json
{"title":"Bad Request","status":400,"detail":"days can be at most 90","errors":[{"field":"days","code":"TOO_LARGE","message":"days can be at most 90"}]}
```

Start in the past:

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{"carType":"SUV","start":"2026-01-01T10:00:00","days":3}'
```

```json
{"title":"Bad Request","status":400,"detail":"Reservation cannot start in the past"}
```

No car free (all 21 vans booked for 1–6 Feb 2027):

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{"carType":"VAN","start":"2027-02-03T09:00:00","days":1}'
```

```json
{"title":"Conflict","status":409,"detail":"No VAN available from 2027-02-03T09:00 to 2027-02-04T09:00"}
```

## Validation

Input is checked in two places, each with its own job:

1. **Request validators** (`reservation/validator`), called by the controllers before anything else.
   They check what the client sent: required fields, rental length, length of `customerId`. They
   collect **all** problems and report them as field errors (`400` with `errors`).
   `RentalRequestValidator` holds the rules shared by availability and booking;
   `CreateReservationRequestValidator` adds the `customerId` check.
2. **`RentalService`** checks the rules that depend on the current time: start not in the past and
   within 12 months. It also rejects a missing car type, start or length, so a caller that skips the
   validator gets a `400` instead of a `NullPointerException`.

`RentalPeriod` is a plain value (start, end) with no checks of its own. It relies on its callers to
pass a valid length.

## Concurrency: why a pessimistic lock

### The problem

Booking is "check, then insert": count the cars already booked for the period, and if one is free,
save a new reservation. Two requests for the last free van can both see one free car, and both insert.
The fleet is then overbooked. This is the main correctness risk of the system, and it gets more likely
the more popular a car type is.

A database constraint cannot prevent it. "No more than N overlapping reservations of a type" is a rule
about many rows and overlapping time ranges, not about a single row or a unique key.

### The solution

`RentalService.reserve` runs in one transaction and starts by locking the `fleet` row of the requested
car type:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select f from FleetEntity f where f.carType = :carType")
Optional<FleetEntity> findForUpdate(@Param("carType") CarType carType);
```

This is `SELECT ... FOR UPDATE`. Another booking of the same type waits at this line until the first
one commits. So for each car type, "count, check, insert" runs as one step, one booking at a time,
and the count always includes every booking committed before.

### Why this lock and not something else

- **Why lock the `fleet` row and not the reservations?** The reservation that would cause
  overbooking does not exist yet, so there is no row to lock. Locking the overlapping reservations
  does not stop a parallel insert of a new one. The `fleet` row exists for every type and stands for
  "the pool of cars of this type", so it is a natural lock per type.
- **Why not optimistic locking (`@Version`)?** It would need a version bump on the fleet row with every
  booking. Under contention, which is exactly when the rule matters, most requests would fail and
  have to retry. A pessimistic lock makes them wait in a queue instead, and each gets a definite answer.
- **Why not `SERIALIZABLE` isolation?** Its behaviour differs between databases (H2 vs PostgreSQL),
  it fails transactions that the client then has to retry, and it affects every query in the
  transaction. A single explicit lock is easier to reason about and to test.
- **Why not a lock in Java (`synchronized`, `ReentrantLock`)?** It works only within one JVM. The
  database lock also holds when several instances of the app share one database.

### What the lock covers and what it does not

- The lock is **per car type**. Bookings of SUVs never wait for bookings of vans.
- Bookings of the same type wait for each other **even for periods that do not overlap**. This keeps
  the code simple. A finer lock (e.g. per type and day) would raise throughput but is much more complex.
- **Availability checks do not lock.** They are informational. The booking checks again under the lock.
- **Cancelling does not lock.** It only frees a car, so it cannot cause overbooking. Two cancellations
  of the same reservation are safe because the update is conditional (`where deleted = false`), and
  only one of them changes the row.

### Lock timeout

The H2 URL sets `LOCK_TIMEOUT=10000`. A booking waits at most 10 seconds for the lock. If it does not
get the lock in time, it fails with `PessimisticLockingFailureException`, which the API answers with
`503 Service Unavailable` and `Retry-After: 1`. This means "busy, try again", not "no car".
`LockTimeoutApiTest` covers it.

`LOCK_TIMEOUT` is an H2 setting. With another database the timeout is set differently, e.g.
`lock_timeout` in PostgreSQL or the `jakarta.persistence.lock.timeout` query hint.

### How it is verified

`RentalServiceConcurrencyTest` runs many bookings of the same type in parallel and checks that
exactly as many succeed as there are cars. It also runs parallel cancellations of one reservation
and checks that exactly one succeeds.

## How availability is calculated

`AvailabilityCalculator.calculatePeakBookingCount` answers: "within this period, what is the largest
number of cars of this type in use at the same moment?"

1. The repository loads only active (not cancelled) reservations of the type that overlap the period.
2. Each reservation becomes two events, `+1` at its start and `-1` at its end, clipped to the period.
3. Events are sorted by time. At the same instant, `-1` comes before `+1`, so a car returned at 10:00
   is free for a rental starting at 10:00.
4. A sweep over the events keeps a running count and records its maximum.

`free cars = fleet size - peak`. Using the peak, not the number of overlapping reservations, matters.
Reservations 1–3 Feb and 4–6 Feb both overlap a 1–6 Feb request, but they never use a car at the same
time, so they take only one car.

The cost is O(k log k) for k overlapping reservations. The repository query uses the index
`idx_reservation_car_type_start_at`.

## Database and Flyway

The schema belongs to Flyway. Hibernate does not create or change tables. It only checks that the
entities match the schema at startup (`spring.jpa.hibernate.ddl-auto: validate`). If an entity and the
schema diverge, the app fails to start instead of failing later at runtime.

Migrations are in `src/main/resources/db/migration`:

| Version | File | Change |
|---|---|---|
| V1 | `V1__create_fleet_and_reservation.sql` | Tables `fleet` and `reservation`, index on `(car_type, start_at)`, initial fleet |
| V2 | `V2__add_reservation_deleted_flag.sql` | Column `reservation.deleted` for soft delete |

Tables:

- `fleet(car_type PK, capacity)`: one row per car type. It is also the row locked during booking.
- `reservation(id UUID PK, car_type, start_at, end_at, customer_id, deleted)`: times are
  `TIMESTAMP WITH TIME ZONE`.

Rules for changing the schema:

- Never edit a migration that has already run. Flyway stores a checksum of each applied migration and
  refuses to start if a file changed.
- Every change is a new file with the next number, e.g. `V3__add_cancelled_at.sql`.
- Data changes go through migrations too. For example, the fleet size is changed with
  `UPDATE fleet SET capacity = ... WHERE car_type = ...` in a new migration.

The database is H2 in memory, so all migrations run on every start, against an empty database. With
a persistent database Flyway would run only the migrations not yet applied.

## Soft delete

Cancelling a reservation does not remove the row. It sets `deleted = true`:

```sql
update reservation set deleted = true where id = ? and deleted = false
```

Why:

- The history stays: you can see what was booked and cancelled, e.g. for reports, disputes or
  analysing demand.
- A mistaken cancellation can be investigated or undone in the database.
- The conditional update makes concurrent cancellations safe without a lock. The database runs it
  atomically, and only one call gets "1 row updated".

What it means for the rest of the code:

- Every read of reservations has to skip cancelled ones. Today these are `findByIdAndDeletedFalse` and
  `findOverlapping` (`... and r.deleted = false`). A new query that forgets the filter would count
  cancelled reservations as booked. Hibernate's `@SQLRestriction` would apply the filter globally;
  it is not used here so that the filter stays visible in each query.
- A cancelled reservation behaves as if it did not exist: `GET` and `DELETE` answer `404`.
- So `DELETE` is not idempotent from the client's point of view. A second call answers `404`, not
  `204`.
- Rows are never removed, so the table only grows. There is no archive or purge job.

## Tests

```bash
./mvnw test
```

| Area | Test classes |
|---|---|
| Domain and calculation | `RentalPeriodTest` (lengths, DST, overlaps), `AvailabilityCalculatorTest` (peak, boundaries) |
| Validation | `CreateReservationRequestValidatorTest`, `AvailabilityRequestValidatorTest` |
| Service with database | `RentalServiceTest` (booking, availability, cancelling, time rules, with a fixed clock) |
| Concurrency | `RentalServiceConcurrencyTest` (no overbooking, single cancellation), `LockTimeoutApiTest` (503) |
| Persistence | `ReservationRepositoryTest`, `JpaFleetInventoryTest` |
| HTTP API | `ReservationApiTest` (status codes, Problem Details, field errors), `ApiExceptionHandlerTest` |

Tests that depend on "now" use a fixed clock (`FixedClockConfig`), so they do not change with the date.

## Limitations and trade-offs

**Storage and deployment**

- H2 in memory: all data is lost on restart. Good for a demo and tests, not for production. For
  production, use a persistent database (e.g. PostgreSQL) and set the lock timeout for it.
- Running several instances only makes sense with a shared database. The lock lives in the database,
  so it would then work across instances.

**Throughput**

- All bookings of one car type run one at a time (see [the lock](#what-the-lock-covers-and-what-it-does-not)).
  This is enough for this scale. With heavy traffic on one type, requests queue up, and after 10
  seconds of waiting they get a `503`.

**Functionality not implemented**

- No authentication or authorization. Whoever knows a reservation id can read and cancel it.
- No list of reservations, e.g. per customer. The web panel remembers ids in the browser only.
- A reservation cannot be changed or extended. You have to cancel it and book again.
- Availability is not a hold. A car shown as free may be gone by the time you book.
- The fleet can be changed only through a migration. Lowering a type's capacity does not check
  existing reservations, which could leave the type overbooked.
- Cancelling has no time rules. A reservation that already started or ended can still be cancelled.
- There is no way to restore a cancelled reservation through the API.

**Time handling**

- One fixed time zone (`Europe/Warsaw`). The API does not accept offsets or other zones.
- A start time that does not exist because clocks go forward (e.g. `2027-03-28T02:30`) is moved
  forward by `java.time` (to 03:30) rather than rejected.
- The "not in the past" and "12 months ahead" rules look only at the start of the rental.

**Errors**

- An unknown car type or malformed JSON answers `400` with a generic `detail`
  (`Failed to read request` / `Invalid request content.`), without the `errors` list.
- The rental length (1–90 days) is checked only by the request validator. Code that calls
  `RentalService` directly, bypassing the controllers, has to validate the request itself.

## Project layout

```
src/main/java/pl/krysinski/carrental
├── CarRentalApplication.java      app entry point, Clock bean (Europe/Warsaw)
├── fleet
│   ├── controller                 GET /api/v1/fleet
│   ├── repository                 FleetEntity, FleetRepository (findForUpdate = the lock)
│   └── service                    FleetInventory, FleetInventoryService
├── reservation
│   ├── controller                 reservations and availability endpoints, ApiExceptionHandler
│   ├── dto                        request and response records
│   ├── entity                     CarType, RentalPeriod, Reservation (domain values)
│   ├── exception                  InvalidReservation, NoAvailability, ReservationNotFound
│   ├── mapper                     MapStruct mappers between entity, domain and DTO
│   ├── repository                 ReservationEntity, ReservationRepository (soft delete)
│   ├── service                    RentalService, AvailabilityCalculator
│   └── validator                  request validators
└── validation                     GenericValidator, error codes, RequestValidationException

src/main/resources
├── application.yml                datasource (H2, LOCK_TIMEOUT), JPA validate mode
├── db/migration                   Flyway migrations
└── static/index.html              web panel
```
