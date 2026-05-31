package simply.simply_study.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import simply.simply_study.dto.*;
import simply.simply_study.exception.ForbiddenException;
import simply.simply_study.exception.InvalidInputException;
import simply.simply_study.exception.ResourceNotFoundException;
import simply.simply_study.model.*;
import simply.simply_study.model.enums.Role;
import simply.simply_study.repository.CourseRepository;
import simply.simply_study.repository.OfferingRepository;
import simply.simply_study.repository.UserRepository;
import simply.simply_study.service.OfferingService;
import simply.simply_study.service.TimezoneService;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferingServiceImpl implements OfferingService {

    private final CourseRepository courseRepository;
    private final OfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final TimezoneService timezoneService;



    @Override
    @Transactional
    public OfferingResponseDto createOffering(CreateOfferingRequest request) {
        log.info("Creating offering for courseId: {}, teacherId: {}, title: '{}', maxCapacity: {}",
                request.courseId(), request.teacherId(), request.title(), request.maxCapacity());

        String trimmedTimezone;
        if (request.timezone() == null || request.timezone().isBlank()) {
            trimmedTimezone = "UTC";
        } else {
            trimmedTimezone = request.timezone().trim();
        }

        try {
            ZoneId.of(trimmedTimezone);
        } catch (Exception e) {
            throw new InvalidInputException("Invalid ZoneId: " + request.timezone());
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        User teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.teacherId()));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("User is not a TEACHER");
        }

        Offering offering = new Offering(course, teacher, request.title(), trimmedTimezone, request.maxCapacity());
        Offering saved = offeringRepository.save(offering);
        log.info("Offering created successfully with id: {}", saved.getId());

        return mapToOfferingResponse(saved, "UTC");
    }

    @Override
    @Transactional
    public List<SessionResponseDto> addSessions(Long offeringId, String teacherId, CreateSessionsRequest request) {
        log.info("Adding sessions to offeringId: {}, teacherId: {}, sessionsCount: {}, timezone: '{}'",
                offeringId, teacherId, request.sessions() != null ? request.sessions().size() : 0, request.timezone());
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with id: " + offeringId));

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("User is not a TEACHER");
        }

        if (!offering.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenException("This teacher does not own this offering");
        }

        String timezoneStr = (request.timezone() == null || request.timezone().isBlank()) ? "UTC" : request.timezone().trim();
        try {
            ZoneId.of(timezoneStr);
        } catch (Exception e) {
            throw new InvalidInputException("Invalid ZoneId: " + request.timezone());
        }

        List<Session> sessions = new ArrayList<>();
        for (SessionTimeDto sessionTimeDto : request.sessions()) {
            Instant startUtc = parseToUtc(sessionTimeDto.startTime(), timezoneStr);
            Instant endUtc = parseToUtc(sessionTimeDto.endTime(), timezoneStr);

            if (startUtc.isAfter(endUtc) || startUtc.equals(endUtc)) {
                throw new InvalidInputException("Session start time must be before end time: " +
                        sessionTimeDto.startTime() + " - " + sessionTimeDto.endTime());
            }

            Session session = new Session(offering, startUtc, endUtc);
            offering.addSession(session);
            sessions.add(session);
        }

        offeringRepository.save(offering);
        log.info("Successfully added and saved {} sessions for offeringId: {}", sessions.size(), offeringId);

        return sessions.stream()
                .map(session -> new SessionResponseDto(
                        session.getId(),
                        offering.getId(),
                        offering.getTeacher().getId(),
                        timezoneService.formatToLocalString(session.getStartTime(), timezoneStr),
                        timezoneService.formatToLocalString(session.getEndTime(), timezoneStr)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferingResponseDto> getTeacherOfferings(String teacherId, String timezone, org.springframework.data.domain.Pageable pageable) {
        log.info("Fetching offerings for teacherId: {}, timezone: '{}', Pageable: {}", teacherId, timezone, pageable);

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("User is not a TEACHER");
        }

        List<Offering> offerings = offeringRepository.findByTeacherIdWithDetails(teacherId);
        String zone = (timezone != null && !timezone.isBlank()) ? timezone : "UTC";

        List<OfferingResponseDto> mapped = offerings.stream()
                .map(o -> mapToOfferingResponse(o, zone))
                .collect(Collectors.toList());

        if (pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), mapped.size());
            if (start > mapped.size()) {
                return List.of();
            }
            return mapped.subList(start, end);
        }
        return mapped;
    }

    @Override
    public List<OfferingResponseDto> getAvailableOfferings(String userId, String timezone, org.springframework.data.domain.Pageable pageable) {
        log.info("Fetching available offerings. User: {}, Timezone: '{}', Pageable: {}", userId, timezone, pageable);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.PARENT) {
            throw new ForbiddenException("User is not a PARENT");
        }

        List<Offering> offerings = offeringRepository.findAllWithSessionsAndCourse();
        String zone = (timezone != null && !timezone.isBlank()) ? timezone : "UTC";
        java.time.Instant now = java.time.Instant.now();

        List<OfferingResponseDto> filtered = offerings.stream()
                .filter(o -> o.getStatus() == simply.simply_study.model.enums.OfferingStatus.PUBLISHED)
                .filter(o -> o.getSessions() != null && o.getSessions().stream().anyMatch(s -> s.getStartTime().isAfter(now)))
                .map(o -> mapToOfferingResponse(o, zone))
                .collect(Collectors.toList());

        if (pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            if (start > filtered.size()) {
                return List.of();
            }
            return filtered.subList(start, end);
        }
        return filtered;
    }

    private OfferingResponseDto mapToOfferingResponse(Offering offering, String timezone) {
        List<SessionResponseDto> sessions = offering.getSessions().stream()
                .map(s -> new SessionResponseDto(
                        s.getId(),
                        offering.getId(),
                        offering.getTeacher().getId(),
                        timezoneService.formatToLocalString(s.getStartTime(), timezone),
                        timezoneService.formatToLocalString(s.getEndTime(), timezone)
                ))
                .collect(Collectors.toList());

        return new OfferingResponseDto(
                offering.getId(),
                offering.getCourse().getId(),
                offering.getCourse().getTitle(),
                offering.getCourse().getDescription(),
                offering.getTeacher().getId(),
                offering.getTitle(),
                offering.getTimezone(),
                offering.getMaxCapacity(),
                (long) offering.getCurrentEnrollment(),
                sessions
        );
    }

    private Instant parseToUtc(String timeStr, String timezoneStr) {
        List<Function<String, Instant>> parsers = List.of(
                s -> Instant.parse(s),
                s -> OffsetDateTime.parse(s).toInstant(),
                s -> ZonedDateTime.parse(s).toInstant()
        );

        for (Function<String, Instant> parser : parsers) {
            try {
                return parser.apply(timeStr);
            } catch (Exception ignored) {
            }
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(timeStr);
            return timezoneService.toUTC(ldt, timezoneStr);
        } catch (Exception e) {
            try {
                LocalDate ld = LocalDate.parse(timeStr);
                return timezoneService.toUTC(ld.atStartOfDay(), timezoneStr);
            } catch (Exception ex) {
                throw new InvalidInputException("Unable to parse time: " + timeStr);
            }
        }
    }
}