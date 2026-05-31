package simply.simply_study.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import simply.simply_study.model.enums.Role;
import simply.simply_study.model.User;
import simply.simply_study.model.Course;
import simply.simply_study.model.Offering;
import simply.simply_study.model.Session;
import simply.simply_study.repository.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class BookingControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSessionRepository bookingSessionRepository;

    private User teacher;
    private User parent;
    private Course course;
    private Offering offering;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        bookingSessionRepository.deleteAll();
        bookingRepository.deleteAll();
        sessionRepository.deleteAll();
        offeringRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(new User("teacher-test-1", "Teacher One", "teacher1@example.com", Role.TEACHER, "UTC"));
        parent = userRepository.save(new User("parent-test-1", "Parent One", "parent1@example.com", Role.PARENT, "UTC"));
        course = courseRepository.save(new Course("Java Programming", "Learn Java 21", teacher));

        offering = offeringRepository.save(new Offering(course, teacher, "Java Basics", "UTC", 10));

        Instant futureStart = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant futureEnd = Instant.now().plus(4, ChronoUnit.HOURS);
        Session session = new Session(offering, futureStart, futureEnd);
        offering.addSession(session);
        sessionRepository.save(session);
    }

    @Test
    public void testCreateBookingSuccessfullyAsParent() throws Exception {
        String requestJson = "{" +
                "\"offeringId\":" + offering.getId() +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", parent.getId())
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Booking created successfully")))
                .andExpect(jsonPath("$.data.offeringId", is(offering.getId().intValue())))
                .andExpect(jsonPath("$.data.teacherId", is(teacher.getId())));
    }

    @Test
    public void testCreateBookingForbiddenAsTeacher() throws Exception {
        String requestJson = "{" +
                "\"offeringId\":" + offering.getId() +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", teacher.getId())
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("User is not a PARENT")));
    }

    @Test
    public void testCreateBookingNotFoundForInvalidParentId() throws Exception {
        String requestJson = "{" +
                "\"offeringId\":" + offering.getId() +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", "non-existent-parent-id")
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Parent not found with id: non-existent-parent-id")));
    }

    @Test
    public void testCreateBookingBadRequestIfNoUserIdProvided() throws Exception {
        String requestJson = "{" +
                "\"offeringId\":" + offering.getId() +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Parent User ID is required in X-User-Id or UserId header")));
    }

    @Test
    public void testCreateBookingValidationErrors() throws Exception {
        String requestJson = "{}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", parent.getId())
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.offeringId", is("Offering ID is required")));
    }

    @Test
    public void testGetBookingsSuccessfullyAsParent() throws Exception {
        String requestJson = "{" +
                "\"offeringId\":" + offering.getId() +
                "}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", parent.getId())
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/bookings")
                        .header("UserId", parent.getId())
                        .header("Timezone", "Asia/Kolkata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Parent bookings fetched successfully")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].offeringId", is(offering.getId().intValue())));
    }

    @Test
    public void testGetBookingsForbiddenAsTeacher() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .header("UserId", teacher.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("User is not a PARENT")));
    }

    @Test
    public void testGetBookingsNotFoundForInvalidParentId() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .header("UserId", "non-existent-parent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Parent not found with id: non-existent-parent-id")));
    }

    @Test
    public void testGetBookingsBadRequestIfNoUserIdProvided() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Parent User ID is required in X-User-Id or UserId header")));
    }
}
