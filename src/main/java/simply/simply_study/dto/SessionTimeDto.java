package simply.simply_study.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionTimeDto(
    @NotBlank(message = "Start time is required")
    String startTime,

    @NotBlank(message = "End time is required")
    String endTime
) {}
