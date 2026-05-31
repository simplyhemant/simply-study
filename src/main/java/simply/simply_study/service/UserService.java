package simply.simply_study.service;

import simply.simply_study.dto.CreateUserRequest;
import simply.simply_study.dto.UserResponseDto;
import simply.simply_study.model.enums.Role;

import java.util.List;

public interface UserService {
    UserResponseDto createUser(CreateUserRequest request);
    UserResponseDto getUserById(String id);
    List<UserResponseDto> getAllUser(org.springframework.data.domain.Pageable pageable);
    List<UserResponseDto> getUserByRole(Role role, org.springframework.data.domain.Pageable pageable);

    default List<UserResponseDto> getAllUser() {
        return getAllUser(org.springframework.data.domain.Pageable.unpaged());
    }

    default List<UserResponseDto> getUserByRole(Role role) {
        return getUserByRole(role, org.springframework.data.domain.Pageable.unpaged());
    }
}