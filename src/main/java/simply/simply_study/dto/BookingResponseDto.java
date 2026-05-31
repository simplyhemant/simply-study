package simply.simply_study.dto;

import java.time.Instant;
import java.util.List;

public record BookingResponseDto(
    Long id,
    Long offeringId,
    String courseTitle,
    String teacherId,
    Instant bookedAt,
    List<SessionResponseDto> sessions
) {}
