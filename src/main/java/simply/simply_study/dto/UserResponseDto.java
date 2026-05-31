package simply.simply_study.dto;

import simply.simply_study.model.enums.Role;

public record UserResponseDto(
    String id,
    String name,
    String email,
    Role role,
    String timezone
) {}
