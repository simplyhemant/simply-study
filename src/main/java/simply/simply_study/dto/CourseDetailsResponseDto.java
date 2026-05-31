package simply.simply_study.dto;

import java.util.List;

public record CourseDetailsResponseDto(
    Long id,
    String title,
    String description,
    List<CourseOfferingDto> offerings
) {
    public record CourseOfferingDto(
        Long id,
        String teacherId,
        String title,
        String timezone,
        Integer maxCapacity,
        Long currentBookings,
        List<SessionResponseDto> sessions
    ) {}
}
