package simply.simply_study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import simply.simply_study.dto.CreateUserRequest;
import simply.simply_study.dto.UserResponseDto;
import simply.simply_study.dto.UserRoleResponse;
import simply.simply_study.model.enums.Role;
import simply.simply_study.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Account Management", description = "Administrative control endpoints for user registration, single account profile data access, and metadata filtering.")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Register System User Profile", description = "Initializes a user registration footprint within the platform relational records, supporting customizable manual UUID values or matching default auto-generation systems.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User profile generated and stored safely in the database structures."),
            @ApiResponse(responseCode = "400", description = "Email address collision or structural identity identifier redundancy conflict detected.")
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<UserResponseDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("REST request to execute 'createUser' for name: {}, role: {}", request.name(), request.role());
        UserResponseDto response = userService.createUser(request);
        return new ResponseEntity<>(simply.simply_study.dto.ApiResponse.success("User registered successfully", response, HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find User Account by ID Key", description = "Performs an explicit database primary row scan lookup based on the passed string user reference identity key token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account details parsed and delivered cleanly."),
            @ApiResponse(responseCode = "404", description = "Target account referenced by string identifier key does not exist inside system directories.")
    })
    public ResponseEntity<simply.simply_study.dto.ApiResponse<UserResponseDto>> getUserById(
            @Parameter(description = "Account verification unique character string key identity mapping descriptor", example = "teacher_101")
            @PathVariable String id) {
        log.info("REST request to execute 'getUserById' for ID: {}", id);
        UserResponseDto response = userService.getUserById(id);
        return ResponseEntity.ok(simply.simply_study.dto.ApiResponse.success("User retrieved successfully", response));
    }

    @GetMapping
    @Operation(summary = "Retrieve System Users List", description = "Fetches a generalized list of system user identities, supporting optional contextual sorting filters based on the Role parameters passed.")
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<UserResponseDto>>> getAllUser(
            @Parameter(description = "Optional functional enum target sorting discriminator constraint", example = "PARENT")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Target offset page count data framework segment indicator", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Dimensional record entry row element baseline window limits size parameter", example = "20")
            @RequestParam(required = false) Integer size) {
        log.info("REST request to execute 'getAllUser' with role filter: {}, page: {}, size: {}", role, page, size);
        org.springframework.data.domain.Pageable pageable = (page != null && size != null)
                ? org.springframework.data.domain.PageRequest.of(page, size)
                : org.springframework.data.domain.Pageable.unpaged();
        List<UserResponseDto> response;
        if (role != null) {
            response = userService.getUserByRole(role, pageable);
        } else {
            response = userService.getAllUser(pageable);
        }
        return ResponseEntity.ok(simply.simply_study.dto.ApiResponse.success("All users retrieved successfully", response));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Fetch Users Separated Natively by Path Role", description = "Dedicated clean pathway to perform explicit categorical structural filtering executions natively mapping to explicit path routing standards.")
    public ResponseEntity<simply.simply_study.dto.ApiResponse<List<UserResponseDto>>> getUserByRole(
            @Parameter(description = "Target group account classification type binding constraint enum", example = "TEACHER")
            @PathVariable Role role,
            @Parameter(description = "Target offset view index descriptor window slice segment", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Total volume limits boundary constraint indicator parameter", example = "10")
            @RequestParam(required = false) Integer size) {
        log.info("REST request to execute 'getUserByRole' for role param: {}, page: {}, size: {}", role, page, size);
        org.springframework.data.domain.Pageable pageable = (page != null && size != null)
                ? org.springframework.data.domain.PageRequest.of(page, size)
                : org.springframework.data.domain.Pageable.unpaged();
        List<UserResponseDto> response = userService.getUserByRole(role, pageable);
        return ResponseEntity.ok(simply.simply_study.dto.ApiResponse.success("Users with role " + role + " retrieved successfully", response));
    }

    @GetMapping("/role")
    @Operation(summary = "Extract Flagged Boolean User Role Struct", description = "Utility endpoint supporting flexible front-end integration schemas to resolve authorization matrix mappings from query formats quickly.")
    public ResponseEntity<simply.simply_study.dto.ApiResponse<UserRoleResponse>> getUserRole(
            @Parameter(description = "Query variable holding identity key descriptor value mappings", example = "user_uuid_99")
            @RequestParam(required = false) String id,
            @Parameter(description = "Alternative query alias variable descriptor holding identity mapping values", example = "user_uuid_99")
            @RequestParam(required = false) String userId) {
        String targetId = (userId != null && !userId.isBlank()) ? userId : id;
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
        log.info("REST request to get role for user: {}", targetId);
        UserResponseDto user = userService.getUserById(targetId);
        UserRoleResponse roleResponse = new UserRoleResponse(
                user.id(),
                user.role(),
                user.role() == Role.TEACHER,
                user.role() == Role.PARENT);
        return ResponseEntity.ok(simply.simply_study.dto.ApiResponse.success("User role retrieved successfully", roleResponse));
    }
}