package simply.simply_study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
    @NotBlank(message = "Course title is required")
    @Size(max = 100, message = "Course title must not exceed 100 characters")
    String title,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description
) {}
