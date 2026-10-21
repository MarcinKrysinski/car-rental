package pl.krysinski.carrental.reservation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Errors nobody planned for still answer in the same ProblemDetail format as the rest of the API.
class ApiExceptionHandlerTest {

    @RestController
    static class FailingController {

        @GetMapping("/fail")
        String fail() {
            throw new IllegalStateException("database password is hunter2");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void shouldAnswerUnexpectedErrorAsProblemDetailWithoutInternals() throws Exception {
        mvc.perform(get("/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("hunter2"))));
    }

    @Test
    void shouldStillAnswerSpringMvcErrorsWithTheirOwnStatus() throws Exception {
        mvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
