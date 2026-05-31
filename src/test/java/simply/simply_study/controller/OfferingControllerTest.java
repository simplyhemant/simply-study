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
import simply.simply_study.repository.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class OfferingControllerTest {

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
    private Course course;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        bookingSessionRepository.deleteAll();
        bookingRepository.deleteAll();
        sessionRepository.deleteAll();
        offeringRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(new User("teacher-1", "Teacher Test", "teacher@example.com", Role.TEACHER, "UTC"));
        course = courseRepository.save(new Course("Java Programming", "Learn Java 21", teacher));
    }

    @Test
    public void testCreateOfferingSuccessfullyAsTeacher() throws Exception {
        String requestJson = "{" +
                "\"courseId\":" + course.getId() + "," +
                "\"title\":\"Advanced Java - Spring 2026 Batch\"," +
                "\"timezone\":\"America/New_York\"," +
                "\"maxCapacity\":15" +
                "}";

        mockMvc.perform(post("/api/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", teacher.getId())
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Offering created successfully")))
                .andExpect(jsonPath("$.data.title", is("Advanced Java - Spring 2026 Batch")))
                .andExpect(jsonPath("$.data.courseId", is(course.getId().intValue())))
                .andExpect(jsonPath("$.data.teacherId", is(teacher.getId())))
                .andExpect(jsonPath("$.data.maxCapacity", is(15)))
                .andExpect(jsonPath("$.data.timezone", is("America/New_York")));
    }

    @Test
    public void testCreateOfferingForbiddenAsParent() throws Exception {
        User parent = userRepository.save(new User("parent-1", "Parent Test", "parent@example.com", Role.PARENT, "UTC"));

        String requestJson = "{" +
                "\"courseId\":" + course.getId() + "," +
                "\"title\":\"Advanced Java - Spring 2026 Batch\"," +
                "\"timezone\":\"America/New_York\"," +
                "\"maxCapacity\":15" +
                "}";

        mockMvc.perform(post("/api/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", parent.getId())
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("User is not a TEACHER")));
    }

    @Test
    public void testCreateOfferingNotFoundForInvalidUserId() throws Exception {
        String requestJson = "{" +
                "\"courseId\":" + course.getId() + "," +
                "\"title\":\"Advanced Java - Spring 2026 Batch\"," +
                "\"timezone\":\"America/New_York\"," +
                "\"maxCapacity\":15" +
                "}";

        mockMvc.perform(post("/api/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", "non-existent-user-id")
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Teacher not found with id: non-existent-user-id")));
    }

    @Test
    public void testCreateOfferingBadRequestIfNoUserIdProvided() throws Exception {
        String requestJson = "{" +
                "\"courseId\":" + course.getId() + "," +
                "\"title\":\"Advanced Java - Spring 2026 Batch\"," +
                "\"timezone\":\"America/New_York\"," +
                "\"maxCapacity\":15" +
                "}";

        mockMvc.perform(post("/api/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Teacher User ID is required in X-User-Id or UserId header")));
    }

    @Test
    public void testCreateOfferingValidationErrors() throws Exception {
        String requestJson = "{" +
                "\"courseId\":" + course.getId() + "," +
                "\"title\":\"\"," +
                "\"timezone\":\"UTC\"," +
                "\"maxCapacity\":0" +
                "}";

        mockMvc.perform(post("/api/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", teacher.getId())
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.title", is("Title is required")))
                .andExpect(jsonPath("$.errors.maxCapacity", is("Max capacity must be at least 1")));
    }

    @Test
    public void testAddSessionsSuccessfullyWithoutTimezone() throws Exception {
        simply.simply_study.model.Offering offering = offeringRepository.save(
                new simply.simply_study.model.Offering(course, teacher, "Test Offering", "UTC", 10)
        );

        String requestJson = "{" +
                "\"sessions\": [" +
                "  {" +
                "    \"startTime\": \"2026-06-15T09:00:00Z\"," +
                "    \"endTime\": \"2026-06-15T11:00:00Z\"" +
                "  }" +
                "]" +
                "}";

        mockMvc.perform(post("/api/offerings/" + offering.getId() + "/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", teacher.getId())
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Sessions added successfully")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].offeringId", is(offering.getId().intValue())))
                .andExpect(jsonPath("$.data[0].teacherId", is(teacher.getId())))
                .andExpect(jsonPath("$.data[0].startTime", containsString("2026-06-15T09:00:00")))
                .andExpect(jsonPath("$.data[0].endTime", containsString("2026-06-15T11:00:00")));
    }

    @Test
    public void testGetOfferingsTeacherAndParentRouting() throws Exception {
        simply.simply_study.model.Offering offering = offeringRepository.save(
                new simply.simply_study.model.Offering(course, teacher, "Teacher's Special Offering", "UTC", 10)
        );
        offering.setStatus(simply.simply_study.model.enums.OfferingStatus.PUBLISHED);
        offeringRepository.save(offering);

        sessionRepository.save(new simply.simply_study.model.Session(
                offering,
                java.time.Instant.parse("2026-06-15T09:00:00Z"),
                java.time.Instant.parse("2026-06-15T11:00:00Z")
        ));

        User parent = userRepository.save(new User("parent-route-test", "Parent Route Test", "parent-route@example.com", Role.PARENT, "UTC"));

        mockMvc.perform(get("/api/offerings")
                        .header("UserId", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Teacher's Special Offering")));

        mockMvc.perform(get("/api/offerings")
                        .header("UserId", parent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Teacher's Special Offering")));
    }
}
