package com.ridelink.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridelink.account.dto.request.LoginRequest;
import com.ridelink.account.dto.request.RegisterRequest;
import com.ridelink.account.dto.request.UpdateStatusRequest;
import com.ridelink.account.dto.response.AccountResponse;
import com.ridelink.account.dto.response.AuthResponse;
import com.ridelink.account.enums.AccountStatus;
import com.ridelink.account.enums.Role;
import com.ridelink.account.exception.EmailAlreadyExistsException;
import com.ridelink.account.exception.GlobalExceptionHandler;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

        private MockMvc mockMvc;

        @Mock
        private AccountService accountService;

        @InjectMocks
        private AccountController accountController;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
                objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        }

        @Test
        @DisplayName("POST /api/accounts/register - Should return 201 Created on valid input")
        void testRegisterSuccessReturns201() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .fullName("John Doe")
                                .email("john@example.com")
                                .password("password123")
                                .phoneNumber("0771234567")
                                .role(Role.PASSENGER)
                                .build();

                AccountResponse response = AccountResponse.builder()
                                .id(1L)
                                .fullName("John Doe")
                                .email("john@example.com")
                                .phoneNumber("0771234567")
                                .role(Role.PASSENGER)
                                .status(AccountStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(accountService.register(any(RegisterRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/accounts/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.email").value("john@example.com"))
                                .andExpect(jsonPath("$.role").value("PASSENGER"));
        }

        @Test
        @DisplayName("POST /api/accounts/register - Should return 400 Bad Request on validation failure")
        void testRegisterValidationFailureReturns400() throws Exception {
                RegisterRequest invalidRequest = RegisterRequest.builder()
                                .fullName("")
                                .email("invalid-email")
                                .password("123") // too short
                                .phoneNumber("")
                                .role(null)
                                .build();

                mockMvc.perform(post("/api/accounts/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors").exists());
        }

        @Test
        @DisplayName("POST /api/accounts/register - Should return 409 Conflict when email exists")
        void testRegisterDuplicateEmailReturns409() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .fullName("John Doe")
                                .email("duplicate@example.com")
                                .password("password123")
                                .phoneNumber("0771234567")
                                .role(Role.PASSENGER)
                                .build();

                when(accountService.register(any(RegisterRequest.class)))
                                .thenThrow(new EmailAlreadyExistsException(
                                                "Email is already registered: duplicate@example.com"));

                mockMvc.perform(post("/api/accounts/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.message")
                                                .value("Email is already registered: duplicate@example.com"));
        }

        @Test
        @DisplayName("POST /api/accounts/login - Should return 200 OK on valid credentials")
        void testLoginSuccessReturns200() throws Exception {
                LoginRequest request = LoginRequest.builder()
                                .email("john@example.com")
                                .password("password123")
                                .build();

                AuthResponse authResponse = AuthResponse.builder()
                                .token("test_jwt_token")
                                .tokenType("Bearer")
                                .id(1L)
                                .email("john@example.com")
                                .fullName("John Doe")
                                .role(Role.PASSENGER)
                                .status(AccountStatus.ACTIVE)
                                .build();

                when(accountService.login(any(LoginRequest.class))).thenReturn(authResponse);

                mockMvc.perform(post("/api/accounts/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("test_jwt_token"))
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("POST /api/accounts/login - Should return 401 Unauthorized on invalid credentials")
        void testLoginInvalidCredentialsReturns401() throws Exception {
                LoginRequest request = LoginRequest.builder()
                                .email("john@example.com")
                                .password("wrongpassword")
                                .build();

                when(accountService.login(any(LoginRequest.class)))
                                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

                mockMvc.perform(post("/api/accounts/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("GET /api/accounts/{id} - Should return 200 OK with account details")
        void testGetAccountByIdReturns200() throws Exception {
                AccountResponse response = AccountResponse.builder()
                                .id(1L)
                                .fullName("John Doe")
                                .email("john@example.com")
                                .phoneNumber("0771234567")
                                .role(Role.PASSENGER)
                                .status(AccountStatus.ACTIVE)
                                .build();

                when(accountService.getAccountById(1L)).thenReturn(response);

                mockMvc.perform(get("/api/accounts/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.fullName").value("John Doe"));
        }

        @Test
        @DisplayName("GET /api/accounts/{id} - Should return 404 Not Found when ID does not exist")
        void testGetAccountByIdNotFoundReturns404() throws Exception {
                when(accountService.getAccountById(999L))
                                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999"));

                mockMvc.perform(get("/api/accounts/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("PATCH /api/accounts/{id}/status - Should return 200 OK on status update")
        void testUpdateStatusReturns200() throws Exception {
                UpdateStatusRequest request = UpdateStatusRequest.builder()
                                .status(AccountStatus.INACTIVE)
                                .build();

                AccountResponse response = AccountResponse.builder()
                                .id(1L)
                                .fullName("John Doe")
                                .email("john@example.com")
                                .phoneNumber("0771234567")
                                .role(Role.PASSENGER)
                                .status(AccountStatus.INACTIVE)
                                .build();

                when(accountService.updateStatus(eq(1L), any(UpdateStatusRequest.class))).thenReturn(response);

                mockMvc.perform(patch("/api/accounts/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("INACTIVE"));
        }
}
