package com.example.userproduct.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        Field secretKeyField = JwtUtil.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(jwtUtil, "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        Field expirationField = JwtUtil.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, 86400000L);
        userDetails = new User("john@example.com", "password", true, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test @DisplayName("generateToken returns non-null JWT")
    void generateToken_returnsNonNullJwt() {
        assertThat(jwtUtil.generateToken(userDetails)).isNotNull().isNotBlank();
    }

    @Test @DisplayName("extractUsername returns correct email")
    void extractUsername_returnsCorrectEmail() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("john@example.com");
    }

    @Test @DisplayName("validateToken returns true for valid token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test @DisplayName("validateToken returns false for wrong user")
    void validateToken_wrongUser_returnsFalse() {
        String token = jwtUtil.generateToken(userDetails);
        UserDetails otherUser = new User("other@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        assertThat(jwtUtil.validateToken(token, otherUser)).isFalse();
    }

    @Test @DisplayName("Expired token throws")
    void isTokenExpired_expiredToken_throws() throws Exception {
        JwtUtil expiredJwtUtil = new JwtUtil();
        Field secretKeyField = JwtUtil.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(expiredJwtUtil, "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        Field expirationField = JwtUtil.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(expiredJwtUtil, 0L);
        String token = expiredJwtUtil.generateToken(userDetails);
        assertThatThrownBy(() -> expiredJwtUtil.validateToken(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test @DisplayName("extractExpiration returns non-null date")
    void extractExpiration_returnsNonNull() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(jwtUtil.extractExpiration(token)).isNotNull();
    }
}
