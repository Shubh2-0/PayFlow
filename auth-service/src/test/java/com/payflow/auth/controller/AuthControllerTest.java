package com.payflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.auth.dto.AuthResponse;
import com.payflow.auth.dto.LoginRequest;
import com.payflow.auth.dto.RegisterRequest;
import com.payflow.auth.dto.UserResponse;
import com.payflow.auth.security.JwtAuthenticationFilter;
import com.payflow.auth.security.JwtService;
import com.payflow.auth.security.SecurityConfig;
import com.payflow.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register - success: should return 201 with auth response")
    void test_register_returnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .fullName("New User")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .tokenType("Bearer")
                .username("newuser")
                .email("new@example.com")
                .role("USER")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/register - invalid input: should return 400")
    void test_register_invalidInput_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("invalid-email")
                .password("")
                .fullName("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - success: should return 200 with auth response")
    void test_login_returnsOk() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .tokenType("Bearer")
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/auth/profile - authenticated: should return 200 with user profile")
    void test_getProfile_authenticated_returnsOk() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .role("USER")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();

        when(authService.getUserProfile("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    @DisplayName("GET /api/auth/profile - unauthenticated: should return 401")
    void test_getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isUnauthorized());
    }
}
