package simply.simply_study.dto;

import simply.simply_study.model.enums.Role;

public record UserRoleResponse(
    String id,
    Role role,
    boolean isTeacher,
    boolean isParent
) {}
