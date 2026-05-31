package simply.simply_study.dto;

import java.util.List;

public record OfferingResponseDto(
    Long id,
    Long courseId,
    String courseTitle,
    String courseDescription,
    String teacherId,
    String title,
    String timezone,
    Integer maxCapacity,
    Long currentBookings,
    List<SessionResponseDto> sessions
) {}
