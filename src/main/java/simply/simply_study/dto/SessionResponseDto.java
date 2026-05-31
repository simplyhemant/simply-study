package simply.simply_study.dto;

public record SessionResponseDto(
    Long id,
    Long offeringId,
    String teacherId,
    String startTime,
    String endTime
) {}
