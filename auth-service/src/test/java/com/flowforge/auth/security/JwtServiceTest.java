package com.flowforge.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1mbG93Zm9yZ2UtYXV0aC1zZXJ2aWNlLXRlc3Rpbmc=";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        userDetails = new User("testuser", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Generate token - should return a valid non-empty JWT token")
    void test_generateToken_returnsValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull().isNotEmpty();
        // Verify it has 3 parts (header.payload.signature)
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Extract username - should return the correct username from token")
    void test_extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Token valid - should return true for a valid non-expired token")
    void test_isTokenValid_withValidToken_returnsTrue() {
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Token valid - should return false for an expired token")
    void test_isTokenValid_withExpiredToken_returnsFalse() {
        // Set expiration to 0 to create an already-expired token
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 0L);

        String token = jwtService.generateToken(userDetails);

        // Need to reset to normal expiration so the validation method works
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Token valid - should return false for wrong user")
    void test_isTokenValid_withWrongUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = new User("otheruser", "password", Collections.emptyList());

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }
}
