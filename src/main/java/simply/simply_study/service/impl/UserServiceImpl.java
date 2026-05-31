package simply.simply_study.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import simply.simply_study.dto.CreateUserRequest;
import simply.simply_study.dto.UserResponseDto;
import simply.simply_study.exception.ResourceNotFoundException;
import simply.simply_study.exception.UserAlreadyExistsException;
import simply.simply_study.model.enums.Role;
import simply.simply_study.model.User;
import simply.simply_study.repository.UserRepository;
import simply.simply_study.service.UserService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponseDto createUser(CreateUserRequest request) {
        log.info("Executing 'createUser' for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email '" + request.email() + "' is already registered");
        }

        String targetId = request.id();
        if (targetId != null && !targetId.isBlank()) {
            if (userRepository.existsById(targetId)) {
                throw new UserAlreadyExistsException("User with ID '" + targetId + "' already exists");
            }
        } else {
            targetId = UUID.randomUUID().toString();
        }

        String tz = request.timezone();
        if (tz == null || tz.isBlank()) {
            tz = "UTC";
        } else {
            tz = tz.trim();
        }
        try {
            java.time.ZoneId.of(tz);
        } catch (Exception e) {
            throw new simply.simply_study.exception.InvalidInputException("Invalid timezone: " + request.timezone());
        }

        User user = new User(targetId, request.name(), request.email(), request.role(), tz);
        User saved = userRepository.save(user);

        return mapToDto(saved);
    }

    @Override
    public UserResponseDto getUserById(String id) {
        log.info("Executing 'getUserById' for ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUser(org.springframework.data.domain.Pageable pageable) {
        log.info("Executing 'getAllUser', Pageable: {}", pageable);
        List<UserResponseDto> mapped = userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        if (pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), mapped.size());
            if (start > mapped.size()) {
                return List.of();
            }
            return mapped.subList(start, end);
        }
        return mapped;
    }

    @Override
    public List<UserResponseDto> getUserByRole(Role role, org.springframework.data.domain.Pageable pageable) {
        log.info("Executing 'getUserByRole' for role: {}, Pageable: {}", role, pageable);
        List<UserResponseDto> mapped = userRepository.findByRole(role).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        if (pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), mapped.size());
            if (start > mapped.size()) {
                return List.of();
            }
            return mapped.subList(start, end);
        }
        return mapped;
    }

    private UserResponseDto mapToDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getTimezone());
    }
}