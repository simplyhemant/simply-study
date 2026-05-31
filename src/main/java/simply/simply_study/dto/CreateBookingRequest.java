package simply.simply_study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    String parentId,

    @NotNull(message = "Offering ID is required")
    Long offeringId
) {}
