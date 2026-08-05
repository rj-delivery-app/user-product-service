package com.example.userproduct.service;

import com.example.userproduct.aop.PublishKafkaEvent;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AuthResponse;
import com.example.userproduct.dto.LoginRequest;
import com.example.userproduct.dto.RegisterRequest;
import com.example.userproduct.entities.User;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for authentication operations.
 *
 * Why: Handles user registration and login, enriching JWT tokens with userId, role,
 *      and name claims for downstream service authorization. Kafka USER_CREATED events
 *      are published declaratively via @PublishKafkaEvent handled by KafkaEventAspect.
 * What: Registers new users with BCrypt-encoded passwords, authenticates credentials,
 *       and generates enriched JWT tokens.
 * Test: Mock UserRepository and JwtUtil; verify token claims include userId, role, and name.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Register a new user
     */
    @Transactional
    @PublishKafkaEvent(eventType = "USER_CREATED", topic = "user")
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .gender(request.getGender())
                .age(request.getAge())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Generate enriched JWT token with userId, role, and name claims
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> extraClaims = buildEnrichedClaims(user);
        String token = jwtUtil.generateToken(extraClaims, userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Authenticate user and generate token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Get user details
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate enriched JWT token with userId, role, and name claims
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        Map<String, Object> extraClaims = buildEnrichedClaims(user);
        String token = jwtUtil.generateToken(extraClaims, userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Build enriched JWT claims with userId, role, and name.
     *
     * Why: Downstream services can extract user identity from the JWT without
     *      making additional calls to the user-product-service.
     */
    private Map<String, Object> buildEnrichedClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());
        return claims;
    }
}
