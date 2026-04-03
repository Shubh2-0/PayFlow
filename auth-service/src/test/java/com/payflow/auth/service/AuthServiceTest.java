package com.payflow.auth.service;

import com.payflow.auth.dto.AuthResponse;
import com.payflow.auth.dto.LoginRequest;
import com.payflow.auth.dto.RegisterRequest;
import com.payflow.auth.dto.UserResponse;
import com.payflow.auth.entity.Role;
import com.payflow.auth.entity.User;
import com.payflow.auth.exception.DuplicateResourceException;
import com.payflow.auth.exception.ResourceNotFoundException;
import com.payflow.auth.repository.UserRepository;
import com.payflow.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userDetails = new org.springframework.security.core.userdetails.User(
                "testuser",
                "encodedPassword",
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Register - success: should create user and return auth response with token")
    void test_register_success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo("USER");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - duplicate username: should throw DuplicateResourceException")
    void test_register_duplicateUsername_throwsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username 'testuser' is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register - duplicate email: should throw DuplicateResourceException")
    void test_register_duplicateEmail_throwsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email 'test@example.com' is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login - success: should authenticate and return auth response with token")
    void test_login_success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("testuser", "password123"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Login - invalid credentials: should throw BadCredentialsException")
    void test_login_invalidCredentials_throwsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Get profile - success: should return user profile")
    void test_getUserProfile_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserResponse response = authService.getUserProfile("testuser");

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Get profile - user not found: should throw ResourceNotFoundException")
    void test_getUserProfile_notFound_throwsException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserProfile("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
