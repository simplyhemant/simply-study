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
import simply.simply_study.dto.*;
import simply.simply_study.exception.InvalidInputException;
import simply.simply_study.service.OfferingService;
import simply.simply_study.service.UserService;
import simply.simply_study.model.enums.Role;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/offerings")
@RequiredArgsConstructor
@Tag(name = "Offering Management", description = "Endpoints for creating and retrieving class offerings and adding schedule slots.")
public class OfferingController {

    private final OfferingService offeringService;
    private final UserService userService;

    @PostMapping
    @Operation(
            summary = "Create a Course Offering (Teacher)",
            description = "Allows a registered TEACHER to create a new class offering/section for a course."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Offering created successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or User ID header missing.", content = @Content),
            @ApiResponse(responseCode = "403", description = "User is not authorized as a TEACHER.", content = @Content),
            @ApiResponse(responseCode = "444", description = "Target course not found.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<OfferingResponseDto>> createOffering(
            @Parameter(description = "Primary authentication header identifying the Teacher user UUID", example = "teacher_abc123")
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,

            @Parameter(description = "Fallback authentication header identifying the Teacher user UUID", example = "teacher_abc123")
            @RequestHeader(value = "UserId", required = false) String userId,

            @Valid @RequestBody CreateOfferingRequest request) {
        String headerUserId = (xUserId != null && !xUserId.isBlank()) ? xUserId : userId;
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new InvalidInputException("Teacher User ID is required in X-User-Id or UserId header");
        }
        log.info("REST request to create offering: {} initiated by teacher: {}", request.title(), headerUserId);
        CreateOfferingRequest populatedRequest = new CreateOfferingRequest(
                request.courseId(),
                headerUserId.trim(),
                request.title(),
                request.timezone(),
                request.maxCapacity()
        );
        OfferingResponseDto responseDto = offeringService.createOffering(populatedRequest);
        simply.simply_study.dto.ApiResponse<OfferingResponseDto> response = simply.simply_study.dto.ApiResponse.success("Offering created successfully", responseDto, HttpStatus.CREATED.value());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
            summary = "Get Offerings dynamically by user role",
            description = "Fetches a list of offerings. If the user is a TEACHER, returns their taught offerings. If the user is a PARENT, returns available offerings they can book."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offerings fetched successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "User ID header missing or invalid request parameters.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<OfferingResponseDto>>> getAvailableOfferings(
            @Parameter(description = "Primary authentication header identifying the user UUID", example = "user_abc123")
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,

            @Parameter(description = "Fallback authentication header identifying the user UUID", example = "user_abc123")
            @RequestHeader(value = "UserId", required = false) String userId,

            @Parameter(description = "Target timezone context to display session schedules", example = "UTC")
            @RequestParam(required = false, defaultValue = "UTC") String timezone,

            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(required = false) Integer size) {
        String headerUserId = (xUserId != null && !xUserId.isBlank()) ? xUserId : userId;
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new InvalidInputException("User ID is required in X-User-Id or UserId header");
        }
        log.info("REST request to get offerings for userId: {} in timezone: '{}', page: {}, size: {}", headerUserId, timezone, page, size);
        org.springframework.data.domain.Pageable pageable = (page != null && size != null)
                ? org.springframework.data.domain.PageRequest.of(page, size)
                : org.springframework.data.domain.Pageable.unpaged();
        
        String trimmedUserId = headerUserId.trim();
        UserResponseDto userDto = userService.getUserById(trimmedUserId);
        List<OfferingResponseDto> responseDto;
        if (userDto.role() == Role.TEACHER) {
            responseDto = offeringService.getTeacherOfferings(trimmedUserId, timezone, pageable);
        } else {
            responseDto = offeringService.getAvailableOfferings(trimmedUserId, timezone, pageable);
        }
        
        simply.simply_study.dto.ApiResponse<List<OfferingResponseDto>> response = simply.simply_study.dto.ApiResponse.success("Offerings fetched successfully", responseDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teacher/{teacherId}")
    @Operation(
            summary = "Get Teacher Offerings (Teacher)",
            description = "Explicitly retrieves all class offerings taught by a specific teacher ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Teacher offerings fetched successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "444", description = "Teacher details do not exist.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<OfferingResponseDto>>> getTeacherOfferings(
            @Parameter(description = "The target Teacher ID to lookup", example = "teacher-123")
            @PathVariable String teacherId,

            @Parameter(description = "Target timezone context to display session schedules", example = "UTC")
            @RequestParam(required = false, defaultValue = "UTC") String timezone,

            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(required = false) Integer size) {
        log.info("REST request to get teacher offerings for teacherId: {} in timezone: '{}', page: {}, size: {}", teacherId, timezone, page, size);
        org.springframework.data.domain.Pageable pageable = (page != null && size != null)
                ? org.springframework.data.domain.PageRequest.of(page, size)
                : org.springframework.data.domain.Pageable.unpaged();
        List<OfferingResponseDto> responseDto = offeringService.getTeacherOfferings(teacherId, timezone, pageable);
        simply.simply_study.dto.ApiResponse<List<OfferingResponseDto>> response = simply.simply_study.dto.ApiResponse.success("Teacher offerings fetched successfully", responseDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/sessions")
    @Operation(
            summary = "Add Sessions to an Offering (Teacher)",
            description = "Adds schedule slots/sessions to an offering. Prevents internal session overlapping conflicts."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sessions added successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or User ID header missing.", content = @Content),
            @ApiResponse(responseCode = "403", description = "User is not the teacher who created this offering.", content = @Content),
            @ApiResponse(responseCode = "444", description = "Offering not found.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Session schedule conflict detected.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<SessionResponseDto>>> addSessions(
            @Parameter(description = "ID of the target offering", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Primary authentication header identifying the Teacher user UUID", example = "teacher_abc123")
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,

            @Parameter(description = "Fallback authentication header identifying the Teacher user UUID", example = "teacher_abc123")
            @RequestHeader(value = "UserId", required = false) String userId,

            @Valid @RequestBody CreateSessionsRequest request) {
        String headerUserId = (xUserId != null && !xUserId.isBlank()) ? xUserId : userId;
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new InvalidInputException("Teacher User ID is required in X-User-Id or UserId header");
        }
        log.info("REST request to add sessions to offeringId: {} by teacher: {}", id, headerUserId);
        List<SessionResponseDto> responseDto = offeringService.addSessions(id, headerUserId.trim(), request);
        simply.simply_study.dto.ApiResponse<List<SessionResponseDto>> response = simply.simply_study.dto.ApiResponse.success("Sessions added successfully", responseDto, HttpStatus.CREATED.value());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
