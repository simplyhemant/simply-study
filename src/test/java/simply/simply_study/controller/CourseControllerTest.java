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
import simply.simply_study.repository.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CourseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingSessionRepository bookingSessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        bookingSessionRepository.deleteAll();
        bookingRepository.deleteAll();
        sessionRepository.deleteAll();
        offeringRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testCreateCourseSuccessfullyAsTeacher() throws Exception {
        User teacher = userRepository.save(new User("teacher-1", "Teacher Test", "teacher@example.com", Role.TEACHER, "UTC"));

        String requestJson = "{" +
                "\"title\":\"Java Basics\"," +
                "\"description\":\"An introductory course to Java.\"" +
                "}";

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", teacher.getId())
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Course created successfully")))
                .andExpect(jsonPath("$.data.title", is("Java Basics")))
                .andExpect(jsonPath("$.data.description", is("An introductory course to Java.")));
    }

    @Test
    public void testCreateCourseForbiddenAsParent() throws Exception {
        User parent = userRepository.save(new User("parent-1", "Parent Test", "parent@example.com", Role.PARENT, "UTC"));

        String requestJson = "{" +
                "\"title\":\"Java Basics\"," +
                "\"description\":\"An introductory course to Java.\"" +
                "}";

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", parent.getId())
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("User is not a TEACHER")));
    }

    @Test
    public void testCreateCourseNotFoundForInvalidUserId() throws Exception {
        String requestJson = "{" +
                "\"title\":\"Java Basics\"," +
                "\"description\":\"An introductory course to Java.\"" +
                "}";

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", "non-existent-user-id")
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("User not found with id: non-existent-user-id")));
    }

    @Test
    public void testCreateCourseBadRequestIfNoUserIdProvided() throws Exception {
        String requestJson = "{" +
                "\"title\":\"Java Basics\"," +
                "\"description\":\"An introductory course to Java.\"" +
                "}";

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateCourseRouteNotFound() throws Exception {
        String requestJson = "{" +
                "\"title\":\"Java Basics\"," +
                "\"description\":\"An introductory course to Java.\"" +
                "}";

        mockMvc.perform(post("/api/courses/some/invalid/subroute/path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("No static resource api/courses/some/invalid/subroute/path")))
                .andExpect(jsonPath("$.path", is("/api/courses/some/invalid/subroute/path")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    public void testGetCourseByIdSuccess() throws Exception {
        User teacher = userRepository.save(new User("teacher-get-course", "Teacher Get Course", "teachergetcourse@example.com", Role.TEACHER, "UTC"));
        simply.simply_study.model.Course course = courseRepository.save(new simply.simply_study.model.Course("Spring Basics", "Description", teacher));

        mockMvc.perform(get("/api/courses/" + course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Course metadata retrieved successfully")))
                .andExpect(jsonPath("$.data.id", is(course.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Spring Basics")));
    }

    @Test
    public void testGetCourseByIdDetailedSuccess() throws Exception {
        User teacher = userRepository.save(new User("teacher-get-course-det", "Teacher Detailed", "teacherdetailed@example.com", Role.TEACHER, "UTC"));
        simply.simply_study.model.Course course = courseRepository.save(new simply.simply_study.model.Course("Spring Detailed", "Description Detailed", teacher));

        mockMvc.perform(get("/api/courses/" + course.getId() + "/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Detailed course schedules retrieved successfully")))
                .andExpect(jsonPath("$.data.id", is(course.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Spring Detailed")))
                .andExpect(jsonPath("$.data.offerings", hasSize(0)));
    }

    @Test
    public void testGetCourseByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/courses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Course not found with id: 99999")));
    }

    @Test
    public void testCreateCourseWithHeadersSuccessfully() throws Exception {
        User teacher = userRepository.save(new User("teacher-header-id", "Teacher Header Test", "teacherheader@example.com", Role.TEACHER, "UTC"));

        String requestJson = "{" +
                "\"title\":\"Java Headers\"," +
                "\"description\":\"Header-based course creation.\"" +
                "}";

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("UserId", teacher.getId())
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Course created successfully")))
                .andExpect(jsonPath("$.data.title", is("Java Headers")));
    }

    @Test
    public void testGetCoursesWithPagination() throws Exception {
        User teacher = userRepository.save(new User("teacher-paged", "Teacher Paged", "teacherpaged@example.com", Role.TEACHER, "UTC"));
        courseRepository.save(new simply.simply_study.model.Course("Course 1", "Desc 1", teacher));
        courseRepository.save(new simply.simply_study.model.Course("Course 2", "Desc 2", teacher));
        courseRepository.save(new simply.simply_study.model.Course("Course 3", "Desc 3", teacher));

        // Test unpaged (default behavior)
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)));

        // Test page 0, size 2
        mockMvc.perform(get("/api/courses?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title", is("Course 1")))
                .andExpect(jsonPath("$.data[1].title", is("Course 2")));

        // Test page 1, size 2
        mockMvc.perform(get("/api/courses?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Course 3")));

        // Test page 2, size 2 (out of bounds)
        mockMvc.perform(get("/api/courses?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
