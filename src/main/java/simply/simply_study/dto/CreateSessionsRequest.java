package simply.simply_study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import java.util.List;

public record CreateSessionsRequest(
    String timezone,

    @NotEmpty(message = "Sessions list cannot be empty")
    @Valid
    List<SessionTimeDto> sessions
) {}
