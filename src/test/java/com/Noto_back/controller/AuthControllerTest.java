package com.Noto_back.controller;

import com.Noto_back.dto.AuthResponse;
import com.Noto_back.dto.LoginRequest;
import com.Noto_back.dto.RefreshRequest;
import com.Noto_back.dto.RegisterRequest;
import com.Noto_back.service.AuthService;
import com.Noto_back.service.CustomUserDetailsService;
import com.Noto_back.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, properties = "app.registration.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authResponse = new AuthResponse("access_token", "refresh_token");
    }

    @Test
    @DisplayName("POST /api/auth/register - 201 Created (Quand inscription activée)")
    void register_succes() throws Exception {
        RegisterRequest request = new RegisterRequest("john_doe", "john@test.com", "Password123!");

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 200 OK")
    void login_succes() throws Exception {
        LoginRequest request = new LoginRequest("john@test.com", "Password123!");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - 200 OK")
    void refresh_succes() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh_token");

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - 204 No Content")
    void logout_succes() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh_token");

        doNothing().when(authService).logout("refresh_token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}