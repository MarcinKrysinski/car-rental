package pl.krysinski.carrental.reservation.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.krysinski.carrental.fleet.FleetController;
import pl.krysinski.carrental.fleet.InMemoryFleetInventory;
import pl.krysinski.carrental.reservation.mapper.AvailabilityMapper;
import pl.krysinski.carrental.reservation.mapper.ReservationMapper;
import pl.krysinski.carrental.reservation.repository.InMemoryReservationRepository;
import pl.krysinski.carrental.reservation.service.RentalService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The HTTP layer on top of a real service, without a Spring context. Fleet: 3 sedans, 2 SUVs, 1 van.
class ReservationApiTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final String RESERVATIONS = "/api/v1/reservations";

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        InMemoryFleetInventory fleet = new InMemoryFleetInventory();
        Clock clock = Clock.fixed(LocalDateTime.of(2027, 6, 1, 8, 0).atZone(WARSAW).toInstant(), WARSAW);
        RentalService service = new RentalService(new InMemoryReservationRepository(), fleet, clock);

        mvc = MockMvcBuilders
                .standaloneSetup(
                        new ReservationController(service, Mappers.getMapper(ReservationMapper.class)),
                        new AvailabilityController(service, Mappers.getMapper(AvailabilityMapper.class)),
                        new FleetController(fleet))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateReservationForRequestedPeriod() throws Exception {
        reserve("VAN", "2027-06-10T10:00:00", 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.carType").value("VAN"))
                .andExpect(jsonPath("$.start").value("2027-06-10T10:00:00"))
                .andExpect(jsonPath("$.end").value("2027-06-12T10:00:00"));
    }

    @Test
    void shouldAnswerConflictWhenNoCarOfTypeIsAvailable() throws Exception {
        reserve("VAN", "2027-06-10T10:00:00", 2);

        reserve("VAN", "2027-06-11T10:00:00", 2)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail", containsString("No VAN available")));
    }

    @Test
    void shouldAnswerBadRequestWhenRentalIsShorterThanOneDay() throws Exception {
        reserve("VAN", "2027-06-10T10:00:00", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("at least one day")));
    }

    @Test
    void shouldAnswerBadRequestWhenStartIsInThePast() throws Exception {
        reserve("VAN", "2027-05-31T10:00:00", 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("past")));
    }

    @Test
    void shouldAnswerBadRequestWhenCarTypeIsMissing() throws Exception {
        mvc.perform(post(RESERVATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"start": "2027-06-10T10:00:00", "days": 2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAnswerBadRequestWhenCarTypeIsUnknown() throws Exception {
        reserve("TRUCK", "2027-06-10T10:00:00", 2)
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnExistingReservation() throws Exception {
        String id = idOf(reserve("SUV", "2027-06-10T10:00:00", 2));

        mvc.perform(get(RESERVATIONS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.carType").value("SUV"))
                .andExpect(jsonPath("$.start").value("2027-06-10T10:00:00"));
    }

    @Test
    void shouldAnswerNotFoundForUnknownReservation() throws Exception {
        mvc.perform(get(RESERVATIONS + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldCancelReservationAndReturnCarToThePool() throws Exception {
        String id = idOf(reserve("VAN", "2027-06-10T10:00:00", 2));

        mvc.perform(delete(RESERVATIONS + "/" + id)).andExpect(status().isNoContent());

        mvc.perform(get(RESERVATIONS + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete(RESERVATIONS + "/" + id)).andExpect(status().isNotFound());
        reserve("VAN", "2027-06-10T10:00:00", 2).andExpect(status().isCreated());
    }

    @Test
    void shouldReportAvailabilityBeforeAndAfterReservation() throws Exception {
        availabilityOfVan()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carType").value("VAN"))
                .andExpect(jsonPath("$.end").value("2027-06-12T10:00:00"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.freeCars").value(1));

        reserve("VAN", "2027-06-10T10:00:00", 2);

        availabilityOfVan()
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.freeCars").value(0));
    }

    @Test
    void shouldListFleetCapacities() throws Exception {
        mvc.perform(get("/api/v1/fleet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.SEDAN").value(3))
                .andExpect(jsonPath("$.SUV").value(2))
                .andExpect(jsonPath("$.VAN").value(1));

        mvc.perform(get("/api/v1/fleet/SUV"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    private ResultActions availabilityOfVan() throws Exception {
        return mvc.perform(get("/api/v1/availability")
                .param("carType", "VAN")
                .param("start", "2027-06-10T10:00:00")
                .param("days", "2"));
    }

    private ResultActions reserve(String carType, String start, int days) throws Exception {
        return mvc.perform(post(RESERVATIONS).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"carType": "%s", "start": "%s", "days": %d}
                        """.formatted(carType, start, days)));
    }

    private static String idOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
