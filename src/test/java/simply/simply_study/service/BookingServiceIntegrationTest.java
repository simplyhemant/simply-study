package simply.simply_study.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import simply.simply_study.dto.*;
import simply.simply_study.exception.OfferingFullException;
import simply.simply_study.exception.ScheduleConflictException;
import simply.simply_study.model.enums.Role;
import simply.simply_study.model.User;
import simply.simply_study.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import simply.simply_study.model.Offering;
import simply.simply_study.service.CourseService;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private OfferingService offeringService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSessionRepository bookingSessionRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private CourseResponseDto course1;
    private CourseResponseDto course2;
    private User teacher1;
    private User teacher2;

    @BeforeEach
    public void setUp() {
        bookingSessionRepository.deleteAll();
        bookingRepository.deleteAll();
        sessionRepository.deleteAll();
        offeringRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Create test teachers
        teacher1 = userRepository.save(new User("teacher1", "Teacher One", "teacher1@simplystudy.com", Role.TEACHER, "Asia/Kolkata"));
        teacher2 = userRepository.save(new User("teacher2", "Teacher Two", "teacher2@simplystudy.com", Role.TEACHER, "UTC"));

        // Create test courses
        course1 = courseService.createCourse(teacher1.getId(), new CreateCourseRequest("Java Programming", "Learn Java 21"));
        course2 = courseService.createCourse(teacher1.getId(), new CreateCourseRequest("Advanced Database Systems", "Deep dive into PostgreSQL"));
    }

    @Test
    public void testCreateOfferingAndSessionsWithTimezone() {
        // Create offering
        CreateOfferingRequest createOfferingReq = new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Intro to Java", "Asia/Kolkata", 10
        );
        var offeringDto = offeringService.createOffering(createOfferingReq);
        assertNotNull(offeringDto.id());

        // Pre-create parent user for available offerings retrieval
        User parent = userRepository.save(new User("parent-test-time", "Parent Test", "parent-test-time@simplystudy.com", Role.PARENT, "UTC"));

        // Add sessions with timezone (e.g. Asia/Kolkata is UTC+5:30)
        List<SessionTimeDto> sessions = List.of(
                new SessionTimeDto("2026-06-01T10:00:00", "2026-06-01T12:00:00") // 10:00 to 12:00 IST -> 04:30 to 06:30 UTC
        );
        CreateSessionsRequest createSessionsReq = new CreateSessionsRequest("Asia/Kolkata", sessions);
        var sessionDtos = offeringService.addSessions(offeringDto.id(), teacher1.getId(), createSessionsReq);

        assertEquals(1, sessionDtos.size());
        // Verify response displays localized time in IST as passed
        assertTrue(sessionDtos.get(0).startTime().contains("2026-06-01T10:00:00+05:30"));
        assertTrue(sessionDtos.get(0).endTime().contains("2026-06-01T12:00:00+05:30"));

        // Fetch using available offerings with a different timezone (e.g. America/New_York is UTC-4 during daylight savings)
        var offeringsInNy = offeringService.getAvailableOfferings(parent.getId(), "America/New_York");
        assertFalse(offeringsInNy.isEmpty());
        var NYsessions = offeringsInNy.get(0).sessions();
        assertEquals(1, NYsessions.size());
        // 04:30 UTC in NY (UTC-4) is 00:30
        assertTrue(NYsessions.get(0).startTime().contains("2026-06-01T00:30:00-04:00"));
        assertTrue(NYsessions.get(0).endTime().contains("2026-06-01T02:30:00-04:00"));
    }

    @Test
    public void testConflictDetectionBetweenBookings() {
        // Pre-create parent user
        String parentId = "parent-conflict-test";
        userRepository.save(new User(parentId, "Conflict Parent", "parent-conflict@simplystudy.com", Role.PARENT, "UTC"));

        // Create 2 offerings
        var offering1 = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Title 1", "UTC", 10
        ));
        var offering2 = offeringService.createOffering(new CreateOfferingRequest(
                course2.id(), teacher2.getId(), "Title 2", "UTC", 10
        ));

        // Add sessions to offering 1: June 1, 10:00 - 11:30 UTC
        offeringService.addSessions(offering1.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto("2026-06-01T10:00:00", "2026-06-01T11:30:00")
        )));

        // Add sessions to offering 2: June 1, 11:00 - 12:00 UTC (overlaps by 30 mins)
        offeringService.addSessions(offering2.id(), teacher2.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto("2026-06-01T11:00:00", "2026-06-01T12:00:00")
        )));

        // Parent books offering 1: should succeed
        var booking1 = bookingService.createBooking(new CreateBookingRequest(parentId, offering1.id()));
        assertNotNull(booking1.id());

        // Parent books offering 2: should fail with ScheduleConflictException
        assertThrows(ScheduleConflictException.class, () -> {
            bookingService.createBooking(new CreateBookingRequest(parentId, offering2.id()));
        });
    }

    @Test
    public void testConcurrentBookingsExceedingCapacity() throws InterruptedException, ExecutionException {
        // Create an offering with capacity = 3
        int capacity = 3;
        var offering = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Capacity Offering", "UTC", capacity
        ));

        // Add a session so we can book
        offeringService.addSessions(offering.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto("2026-06-01T10:00:00", "2026-06-01T11:30:00")
        )));

        int concurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger capacityExceededCounter = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        // Pre-create parents
        for (int i = 0; i < concurrentRequests; i++) {
            final String pid = "concurrent-parent-" + i;
            userRepository.save(new User(pid, "Concurrent Parent " + i, pid + "@simplystudy.com", Role.PARENT, "UTC"));
        }

        for (int i = 0; i < concurrentRequests; i++) {
            final String pid = "concurrent-parent-" + i;
            futures.add(executor.submit(() -> {
                latch.await(); // Wait for trigger
                try {
                    bookingService.createBooking(new CreateBookingRequest(pid, offering.id()));
                    successCounter.incrementAndGet();
                    return true;
                } catch (OfferingFullException ex) {
                    capacityExceededCounter.incrementAndGet();
                    return false;
                } catch (Exception ex) {
                    otherErrors.incrementAndGet();
                    return false;
                }
            }));
        }

        // Start all threads at once
        latch.countDown();

        // Wait for all to complete
        for (var f : futures) {
            f.get();
        }
        executor.shutdown();

        // Verify count of bookings in DB
        long dbBookingCount = bookingRepository.countByOfferingId(offering.id());
        assertEquals(capacity, dbBookingCount);
        assertEquals(capacity, successCounter.get());
        assertEquals(concurrentRequests - capacity, capacityExceededCounter.get());
        assertEquals(0, otherErrors.get());
    }

    @Test
    public void testConcurrentBookingsForSameParentWithOverlappingSessions() throws InterruptedException, ExecutionException {
        // Pre-create parent user
        String parentId = "same-parent-concurrency";
        userRepository.save(new User(parentId, "Same Parent", "sameparent@simplystudy.com", Role.PARENT, "UTC"));

        // Create 2 offerings
        var offering1 = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Title 1", "UTC", 10
        ));
        var offering2 = offeringService.createOffering(new CreateOfferingRequest(
                course2.id(), teacher2.getId(), "Title 2", "UTC", 10
        ));

        // Add sessions to offering 1: June 1, 10:00 - 11:30 UTC
        offeringService.addSessions(offering1.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto("2026-06-01T10:00:00", "2026-06-01T11:30:00")
        )));

        // Add sessions to offering 2: June 1, 11:00 - 12:00 UTC (overlaps by 30 mins)
        offeringService.addSessions(offering2.id(), teacher2.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto("2026-06-01T11:00:00", "2026-06-01T12:00:00")
        )));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Boolean> booking1Future = executor.submit(() -> {
            latch.await();
            try {
                bookingService.createBooking(new CreateBookingRequest(parentId, offering1.id()));
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        Future<Boolean> booking2Future = executor.submit(() -> {
            latch.await();
            try {
                bookingService.createBooking(new CreateBookingRequest(parentId, offering2.id()));
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // Trigger simultaneous execution
        latch.countDown();

        boolean booking1Succeeded = booking1Future.get();
        boolean booking2Succeeded = booking2Future.get();

        executor.shutdown();

        // One must succeed and one must fail (resulting in exactly one successful booking)
        assertTrue(booking1Succeeded ^ booking2Succeeded,
                "Exactly one of the overlapping bookings must have succeeded");

        long dbBookingCount = bookingRepository.count();
        assertEquals(1, dbBookingCount, "There should be exactly 1 booking in the database due to conflict validation");
    }

    @Test
    public void testCreateOfferingWithInvalidTeacherThrowsException() {
        assertThrows(simply.simply_study.exception.ResourceNotFoundException.class, () -> {
            offeringService.createOffering(new CreateOfferingRequest(
                    course1.id(), "invalid-teacher-id", "Title", "UTC", 10
            ));
        });
    }

    @Test
    public void testGetTeacherOfferingsWithInvalidTeacherThrowsException() {
        assertThrows(simply.simply_study.exception.ResourceNotFoundException.class, () -> {
            offeringService.getTeacherOfferings("invalid-teacher-id", "UTC");
        });
    }

    @Test
    public void testGetTeacherOfferingsSuccess() {
        var offering = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Title", "UTC", 10
        ));
        var offerings = offeringService.getTeacherOfferings(teacher1.getId(), "UTC");
        assertEquals(1, offerings.size());
        assertEquals(offering.id(), offerings.get(0).id());
        assertEquals(teacher1.getId(), offerings.get(0).teacherId());
    }

    @Test
    public void testCreateBookingForPastOfferingThrowsException() {
        User parent = userRepository.save(new User("parent-past-test", "Parent Past", "parent-past@example.com", Role.PARENT, "UTC"));
        var offering = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Past Offering", "UTC", 10
        ));
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        java.time.LocalDateTime pastStart = java.time.LocalDateTime.ofInstant(java.time.Instant.now().minus(java.time.Duration.ofHours(2)), java.time.ZoneOffset.UTC);
        java.time.LocalDateTime pastEnd = java.time.LocalDateTime.ofInstant(java.time.Instant.now().minus(java.time.Duration.ofHours(1)), java.time.ZoneOffset.UTC);
        String pastStartStr = pastStart.format(dtf);
        String pastEndStr = pastEnd.format(dtf);

        offeringService.addSessions(offering.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto(pastStartStr, pastEndStr)
        )));

        assertThrows(ScheduleConflictException.class, () -> {
            bookingService.createBooking(new CreateBookingRequest(parent.getId(), offering.id()));
        });
    }

    @Test
    public void testGetAvailableOfferingsFiltersDraftAndExpired() {
        User parent = userRepository.save(new User("parent-avail-test", "Parent Avail", "parent-avail@example.com", Role.PARENT, "UTC"));

        var pubFuture = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Pub Future", "UTC", 10
        ));
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        java.time.LocalDateTime futureStart = java.time.LocalDateTime.ofInstant(java.time.Instant.now().plus(java.time.Duration.ofHours(2)), java.time.ZoneOffset.UTC);
        java.time.LocalDateTime futureEnd = java.time.LocalDateTime.ofInstant(java.time.Instant.now().plus(java.time.Duration.ofHours(3)), java.time.ZoneOffset.UTC);
        offeringService.addSessions(pubFuture.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto(futureStart.format(dtf), futureEnd.format(dtf))
        )));

        var pubExpired = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Pub Expired", "UTC", 10
        ));
        java.time.LocalDateTime pastStart = java.time.LocalDateTime.ofInstant(java.time.Instant.now().minus(java.time.Duration.ofHours(3)), java.time.ZoneOffset.UTC);
        java.time.LocalDateTime pastEnd = java.time.LocalDateTime.ofInstant(java.time.Instant.now().minus(java.time.Duration.ofHours(2)), java.time.ZoneOffset.UTC);
        offeringService.addSessions(pubExpired.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto(pastStart.format(dtf), pastEnd.format(dtf))
        )));

        var draftFuture = offeringService.createOffering(new CreateOfferingRequest(
                course1.id(), teacher1.getId(), "Draft Future", "UTC", 10
        ));
        offeringService.addSessions(draftFuture.id(), teacher1.getId(), new CreateSessionsRequest("UTC", List.of(
                new SessionTimeDto(futureStart.format(dtf), futureEnd.format(dtf))
        )));
        Offering draftOffering = offeringRepository.findById(draftFuture.id()).orElseThrow();
        draftOffering.setStatus(simply.simply_study.model.enums.OfferingStatus.DRAFT);
        offeringRepository.save(draftOffering);

        var available = offeringService.getAvailableOfferings(parent.getId(), "UTC");

        List<Long> returnedIds = available.stream().map(simply.simply_study.dto.OfferingResponseDto::id).collect(Collectors.toList());
        assertTrue(returnedIds.contains(pubFuture.id()), "Should contain published future offering");
        assertFalse(returnedIds.contains(pubExpired.id()), "Should not contain expired offering");
        assertFalse(returnedIds.contains(draftFuture.id()), "Should not contain draft offering");
    }
}
