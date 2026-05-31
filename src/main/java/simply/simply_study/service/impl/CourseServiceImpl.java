package simply.simply_study.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import simply.simply_study.dto.*;
import simply.simply_study.exception.ForbiddenException;
import simply.simply_study.exception.ResourceNotFoundException;
import simply.simply_study.model.Course;
import simply.simply_study.model.User;
import simply.simply_study.model.enums.Role;
import simply.simply_study.repository.CourseRepository;
import simply.simply_study.repository.UserRepository;
import simply.simply_study.service.CourseService;
import simply.simply_study.service.TimezoneService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final TimezoneService timezoneService;

    private CourseResponseDto mapToCourseResponse(Course course) {
        return new CourseResponseDto(course.getId(), course.getTitle(), course.getDescription());
    }

    @Override
    @Transactional
    public CourseResponseDto createCourse(String teacherId, CreateCourseRequest request) {
        log.info("Creating course with title: '{}' by resolving teacher UUID: '{}'", request.title(), teacherId);

        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> {
                    log.warn("Course creation rejected: User UUID '{}' does not exist.", teacherId);
                    return new ResourceNotFoundException("User not found with id: " + teacherId);
                });

        if (user.getRole() != Role.TEACHER) {
            log.warn("Security Alert: User '{}' with resolved role '{}' attempted unauthorized course creation.",
                    teacherId, user.getRole());
            throw new ForbiddenException("User is not a TEACHER");
        }

        Course course = new Course(request.title(), request.description(), user);
        Course saved = courseRepository.save(course);

        log.info("Course successfully created by authorized Teacher '{}'. Persistent ID: {}", user.getId(), saved.getId());
        return mapToCourseResponse(saved);
    }

    @Override
    public List<CourseResponseDto> getAllCourses(org.springframework.data.domain.Pageable pageable) {
        log.info("Fetching all lightweight courses, Pageable: {}", pageable);
        List<CourseResponseDto> mapped = courseRepository.findAll().stream()
                .map(this::mapToCourseResponse)
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
    public CourseResponseDto getCourseById(Long id) {
        log.info("Fetching lightweight course metadata by ID: {}", id);
        return courseRepository.findById(id)
                .map(this::mapToCourseResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    @Override
    public CourseDetailsResponseDto getCourseDetailsById(Long id, String timezone) {
        log.info("Fetching single course with detailed nested offerings by ID: {} in timezone: {}", id, timezone);
        String zone = (timezone != null && !timezone.isBlank()) ? timezone : "UTC";

        return courseRepository.findByIdWithDetails(id)
                .map(course -> mapToCourseDetailsResponse(course, zone))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private CourseDetailsResponseDto mapToCourseDetailsResponse(Course course, String timezone) {
        List<CourseDetailsResponseDto.CourseOfferingDto> offeringDtos = course.getOfferings().stream()
                .map(offering -> {
                    List<SessionResponseDto> sessionDtos = offering.getSessions().stream()
                            .map(s -> new SessionResponseDto(
                                    s.getId(),
                                    offering.getId(),
                                    offering.getTeacher().getId(),
                                    timezoneService.formatToLocalString(s.getStartTime(), timezone),
                                    timezoneService.formatToLocalString(s.getEndTime(), timezone)
                            ))
                            .collect(Collectors.toList());

                    return new CourseDetailsResponseDto.CourseOfferingDto(
                            offering.getId(),
                            offering.getTeacher().getId(),
                            offering.getTitle(),
                            offering.getTimezone(),
                            offering.getMaxCapacity(),
                            (long) offering.getCurrentEnrollment(),
                            sessionDtos
                    );
                })
                .collect(Collectors.toList());

        return new CourseDetailsResponseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                offeringDtos
        );
    }
}
