package simply.simply_study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import simply.simply_study.dto.CourseResponseDto;
import simply.simply_study.dto.CourseDetailsResponseDto;
import simply.simply_study.dto.CreateCourseRequest;
import simply.simply_study.exception.InvalidInputException;
import simply.simply_study.service.CourseService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Endpoints for creating and retrieving courses.")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(
            summary = "Create a Course (Teacher)",
            description = "Allows an authorized Teacher to create a new learning course."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or User ID header missing.", content = @Content),
            @ApiResponse(responseCode = "403", description = "User is not authorized as a TEACHER.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<CourseResponseDto>> createCourse(
            @Parameter(description = "Primary authentication header identifying the Teacher user UUID", example = "teacher_abc123")
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,

            @Parameter(description = "Fallback authentication header identifying the Teacher user UUID", example = "teacher_abc123")
            @RequestHeader(value = "UserId", required = false) String userId,

            @Valid @RequestBody CreateCourseRequest request) {

        String headerUserId = (xUserId != null && !xUserId.isBlank()) ? xUserId : userId;

        if (headerUserId == null || headerUserId.isBlank()) {
            throw new InvalidInputException("User ID (UUID) is required in the request headers (X-User-Id or UserId)");
        }

        log.info("REST request to create course: '{}' initiated by User UUID: {}", request.title(), headerUserId);
        CourseResponseDto course = courseService.createCourse(headerUserId.trim(), request);

        simply.simply_study.dto.ApiResponse<CourseResponseDto> response = simply.simply_study.dto.ApiResponse.success("Course created successfully", course);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
            summary = "Get All Courses",
            description = "Retrieves a list of courses with optional pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses fetched successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class)))
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<CourseResponseDto>>> getAllCourses(
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(required = false) Integer size) {
        log.info("REST request to get all courses (lightweight metadata list) with page: {}, size: {}", page, size);
        org.springframework.data.domain.Pageable pageable = (page != null && size != null)
                ? org.springframework.data.domain.PageRequest.of(page, size)
                : org.springframework.data.domain.Pageable.unpaged();
        List<CourseResponseDto> courses = courseService.getAllCourses(pageable);
        simply.simply_study.dto.ApiResponse<List<CourseResponseDto>> response = simply.simply_study.dto.ApiResponse.success("Courses fetched successfully", courses);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Course by ID",
            description = "Retrieves lightweight course metadata by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course metadata retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<CourseResponseDto>> getCourseById(
            @Parameter(description = "ID of the target course", example = "1")
            @PathVariable Long id) {
        log.info("REST request to get lightweight course metadata by ID: {}", id);
        CourseResponseDto course = courseService.getCourseById(id);
        simply.simply_study.dto.ApiResponse<CourseResponseDto> response = simply.simply_study.dto.ApiResponse.success("Course metadata retrieved successfully", course);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/detailed")
    @Operation(
            summary = "Get Detailed Course View",
            description = "Retrieves detailed course view including all scheduled offerings and sessions formatted in a specific timezone."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detailed course schedules retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<CourseDetailsResponseDto>> getCourseByIdDetailed(
            @Parameter(description = "ID of the target course", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Target timezone context for session schedules", example = "UTC")
            @RequestParam(required = false, defaultValue = "UTC") String timezone) {
        log.info("REST request to get detailed course view by ID: {} in timezone: {}", id, timezone);
        CourseDetailsResponseDto courseDetails = courseService.getCourseDetailsById(id, timezone);
        simply.simply_study.dto.ApiResponse<CourseDetailsResponseDto> response = simply.simply_study.dto.ApiResponse.success("Detailed course schedules retrieved successfully", courseDetails);
        return ResponseEntity.ok(response);
    }
}