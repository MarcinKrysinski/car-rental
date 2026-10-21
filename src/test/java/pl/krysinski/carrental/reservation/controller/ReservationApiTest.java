package pl.krysinski.carrental.reservation.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pl.krysinski.carrental.FixedClockConfig;
import pl.krysinski.carrental.reservation.validator.RentalRequestValidator;
import pl.krysinski.carrental.fleet.FleetEntity;
import pl.krysinski.carrental.fleet.FleetRepository;
import pl.krysinski.carrental.reservation.entity.CarType;
import pl.krysinski.carrental.reservation.repository.ReservationEntity;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static pl.krysinski.carrental.reservation.entity.CarType.SEDAN;
import static pl.krysinski.carrental.reservation.entity.CarType.SUV;
import static pl.krysinski.carrental.reservation.entity.CarType.VAN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The whole application over HTTP, against H2 with "now" frozen at 2027-06-01 08:00; every test is rolled back.
// Fleet: 3 sedans, 2 SUVs, 1 van.
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
@Transactional
class ReservationApiTest {

    private static final String RESERVATIONS = "/api/v1/reservations";
    private static final Map<CarType, Integer> FLEET = Map.of(SEDAN, 3, SUV, 2, VAN, 1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private FleetRepository fleetRepository;

    @BeforeEach
    void setUpFleet() {
        FLEET.forEach((carType, capacity) -> fleetRepository.save(new FleetEntity(carType, capacity)));
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
                .andExpect(jsonPath("$.errors[0].field").value("days"))
                .andExpect(jsonPath("$.errors[0].code").value("TOO_SMALL"))
                .andExpect(jsonPath("$.errors[0].message").value("days must be at least 1"));
    }

    @Test
    void shouldAnswerBadRequestWhenRentalIsLongerThanMaximum() throws Exception {
        reserve("VAN", "2027-06-10T10:00:00", Integer.MAX_VALUE)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("days"))
                .andExpect(jsonPath("$.errors[0].code").value("TOO_LARGE"))
                .andExpect(jsonPath("$.errors[0].message").value("days can be at most " + RentalRequestValidator.MAX_DAYS));
    }

    @Test
    void shouldAnswerBadRequestWhenCustomerIdIsTooLong() throws Exception {
        mvc.perform(post(RESERVATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType": "VAN", "start": "2027-06-10T10:00:00", "days": 2, "customerId": "%s"}
                                """.formatted("x".repeat(ReservationEntity.CUSTOMER_ID_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldAnswerBadRequestWhenStartIsBeyondBookingHorizon() throws Exception {
        reserve("VAN", "+999999999-12-31T10:00:00", 2)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("months ahead")));

        mvc.perform(get("/api/v1/availability")
                        .param("carType", "VAN")
                        .param("start", "+999999999-12-31T10:00:00")
                        .param("days", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("months ahead")));
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
    void shouldListEveryInvalidFieldWithItsCode() throws Exception {
        mvc.perform(post(RESERVATIONS).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"start": "2027-06-10T10:00:00", "customerId": "%s"}
                                """.formatted("x".repeat(ReservationEntity.CUSTOMER_ID_LENGTH + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("carType"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"))
                .andExpect(jsonPath("$.errors[0].message").value("carType is required"))
                .andExpect(jsonPath("$.errors[1].field").value("days"))
                .andExpect(jsonPath("$.errors[1].code").value("REQUIRED"))
                .andExpect(jsonPath("$.errors[2].field").value("customerId"))
                .andExpect(jsonPath("$.errors[2].code").value("TOO_LONG"));
    }

    @Test
    void shouldAnswerBadRequestWhenCarTypeIsUnknown() throws Exception {
        reserve("TRUCK", "2027-06-10T10:00:00", 2)
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAnswerBadRequestWithErrorsWhenAvailabilityParamsAreMissingOrInvalid() throws Exception {
        mvc.perform(get("/api/v1/availability").param("start", "2027-06-10T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("carType"))
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"))
                .andExpect(jsonPath("$.errors[1].field").value("days"))
                .andExpect(jsonPath("$.errors[1].code").value("REQUIRED"));

        mvc.perform(get("/api/v1/availability")
                        .param("carType", "VAN")
                        .param("start", "2027-05-31T10:00:00")
                        .param("days", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("past")));

        mvc.perform(get("/api/v1/availability")
                        .param("carType", "TRUCK")
                        .param("start", "2027-06-10T10:00:00")
                        .param("days", "2"))
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
