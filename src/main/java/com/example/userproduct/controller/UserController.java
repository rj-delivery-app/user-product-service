package com.example.userproduct.controller;

import com.example.userproduct.dto.UpdateUserProfileRequest;
import com.example.userproduct.dto.UserProfileResponse;
import com.example.userproduct.service.UserContextService;
import com.example.userproduct.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user management endpoints
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management endpoints")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;
    private final UserContextService userContextService;

    @GetMapping("/profile")
    @Operation(
        summary = "Get current user's profile",
        description = "Get the profile of the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        String userId = userContextService.getCurrentUserId();
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Update current user's profile",
        description = "Update the profile of the currently authenticated user (name, phone, gender, age)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        String userId = userContextService.getCurrentUserId();
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/riders")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'ADMIN')")
    @Operation(
        summary = "List available delivery riders",
        description = "List all active delivery riders. Requires MERCHANT_ADMIN or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Riders retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<java.util.List<UserProfileResponse>> getAvailableRiders() {
        return ResponseEntity.ok(userService.getRidersList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(
        summary = "Get user by ID",
        description = "Get user details by ID. Requires MERCHANT_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        UserProfileResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(
        summary = "List all users",
        description = "List all users with pagination. Requires MERCHANT_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserProfileResponse> response = userService.getAllUsers(pageNo, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(
        summary = "Deactivate user",
        description = "Soft-delete a user by setting isActive to false. Requires MERCHANT_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User deactivated successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> deactivateUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        UserProfileResponse response = userService.deactivateUser(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(
        summary = "Activate user",
        description = "Reactivate a deactivated user. Requires MERCHANT_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User activated successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> activateUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        UserProfileResponse response = userService.activateUser(id);
        return ResponseEntity.ok(response);
    }
}
