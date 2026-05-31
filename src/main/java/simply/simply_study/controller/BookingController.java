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
import simply.simply_study.dto.BookingResponseDto;
import simply.simply_study.dto.CreateBookingRequest;
import simply.simply_study.exception.InvalidInputException;
import simply.simply_study.service.BookingService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Endpoints for parents to book course sections and manage existing class schedules.")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(
            summary = "Book a Course Offering (parent)",
            description = "Allows a registered Parent to enroll into an entire course section. Automatically maps all underlying class sessions, checks capacity limits, and enforces schedule conflict detection rules."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or User is not registered as a PARENT.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Target course offering or Parent user record not found.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Schedule conflict or duplicate enrollment detected for the parent.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<BookingResponseDto>> createBooking(
            @Parameter(description = "Primary authentication header identifying the Parent user UUID", example = "parent_abc123")
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,

            @Parameter(description = "Fallback authentication header identifying the Parent user UUID", example = "parent_abc123")
            @RequestHeader(value = "UserId", required = false) String userId,

            @Valid @RequestBody CreateBookingRequest request) {

        String headerUserId = (xUserId != null && !xUserId.isBlank()) ? xUserId : userId;
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new InvalidInputException("Parent User ID is required in X-User-Id or UserId header");
        }

        CreateBookingRequest populatedRequest = new CreateBookingRequest(headerUserId.trim(), request.offeringId());
        log.info("REST request to create booking for parentId: {}, offeringId: {}", populatedRequest.parentId(), populatedRequest.offeringId());

        BookingResponseDto responseDto = bookingService.createBooking(populatedRequest);
        simply.simply_study.dto.ApiResponse<BookingResponseDto> response = simply.simply_study.dto.ApiResponse.success("Booking created successfully", responseDto, HttpStatus.CREATED.value());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
            summary = "Get Booked Offerings for a Parent",
            description = "Fetches all active class section enrollments registered to a specific parent. Timestamps are shifted on-the-fly into the specified target timezone context."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parent booking schedule retrieved successfully.",
                    content = @Content(schema = @Schema(implementation = simply.simply_study.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parent user validation failed or invalid input arguments.", content = @Content),
            @ApiResponse(responseCode = "444", description = "The requested Parent user details do not exist.", content = @Content)
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<BookingResponseDto>>> getParentBookings(
            @Parameter(description = "Primary authentication header identifying the Parent user UUID", example = "parent_abc123")
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,

            @Parameter(description = "Fallback authentication header identifying the Parent user UUID", example = "parent_abc123")
            @RequestHeader(value = "UserId", required = false) String userId,

            @Parameter(description = "Optional custom target zone string provided in header", example = "Asia/Kolkata")
            @RequestHeader(value = "Timezone", required = false) String headerTimezone,

            @Parameter(description = "Optional custom target zone string provided as url parameter query descriptor", example = "America/New_York")
            @RequestParam(required = false) String timezone) {

        String headerUserId = (xUserId != null && !xUserId.isBlank()) ? xUserId : userId;
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new InvalidInputException("Parent User ID is required in X-User-Id or UserId header");
        }

        String resolvedTimezone = "UTC";
        if (headerTimezone != null && !headerTimezone.isBlank()) {
            resolvedTimezone = headerTimezone;
        } else if (timezone != null && !timezone.isBlank()) {
            resolvedTimezone = timezone;
        }

        log.info("REST request to get booked offerings for parentId: {} with timezone: {}", headerUserId, resolvedTimezone);

        List<BookingResponseDto> responseDto = bookingService.getParentBookings(headerUserId.trim(), resolvedTimezone);
        simply.simply_study.dto.ApiResponse<List<BookingResponseDto>> response = simply.simply_study.dto.ApiResponse.success("Parent bookings fetched successfully", responseDto);
        return ResponseEntity.ok(response);
    }
}