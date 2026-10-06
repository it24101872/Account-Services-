package com.ridelink.account.service;

import com.ridelink.account.dto.request.LoginRequest;
import com.ridelink.account.dto.request.RegisterRequest;
import com.ridelink.account.dto.request.UpdateProfileRequest;
import com.ridelink.account.dto.request.UpdateStatusRequest;
import com.ridelink.account.dto.response.AccountResponse;
import com.ridelink.account.dto.response.AuthResponse;
import com.ridelink.account.enums.AccountStatus;
import com.ridelink.account.enums.Role;
import com.ridelink.account.exception.AccountInactiveException;
import com.ridelink.account.exception.EmailAlreadyExistsException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.exception.UnauthorizedAccessException;
import com.ridelink.account.model.Account;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtTokenProvider;
import com.ridelink.account.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account mockPassengerAccount;
    private Account mockDriverAccount;

    @BeforeEach
    void setUp() {
        mockPassengerAccount = Account.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .phoneNumber("0771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockDriverAccount = Account.builder()
                .id(2L)
                .fullName("Jane Smith")
                .email("jane@example.com")
                .password("encoded_pass")
                .phoneNumber("0777654321")
                .role(Role.DRIVER)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should register passenger successfully")
    void testRegisterPassengerSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("password123")
                .phoneNumber("0771234567")
                .role(Role.PASSENGER)
                .build();

        when(accountRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(accountRepository.save(any(Account.class))).thenReturn(mockPassengerAccount);

        AccountResponse response = accountService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getFullName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Should register driver successfully")
    void testRegisterDriverSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Smith")
                .email("jane@example.com")
                .password("password123")
                .phoneNumber("0777654321")
                .role(Role.DRIVER)
                .build();

        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(accountRepository.save(any(Account.class))).thenReturn(mockDriverAccount);

        AccountResponse response = accountService.register(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(Role.DRIVER, response.getRole());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("password123")
                .phoneNumber("0771234567")
                .role(Role.PASSENGER)
                .build();

        when(accountRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> accountService.register(request));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockPassengerAccount));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "john@example.com", "PASSENGER", "John Doe"))
                .thenReturn("mock_jwt_token");

        AuthResponse response = accountService.login(request);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1L, response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when account does not exist")
    void testLoginUserNotFoundThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@example.com")
                .password("password123")
                .build();

        when(accountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(request));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void testLoginWrongPasswordThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockPassengerAccount));
        when(passwordEncoder.matches("wrongpassword", "encoded_pass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(request));
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when account status is INACTIVE")
    void testLoginInactiveAccountThrowsException() {
        mockPassengerAccount.setStatus(AccountStatus.INACTIVE);
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockPassengerAccount));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        assertThrows(AccountInactiveException.class, () -> accountService.login(request));
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when account status is SUSPENDED")
    void testLoginSuspendedAccountThrowsException() {
        mockPassengerAccount.setStatus(AccountStatus.SUSPENDED);
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockPassengerAccount));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        assertThrows(AccountInactiveException.class, () -> accountService.login(request));
    }

    @Test
    @DisplayName("Should get account by ID successfully")
    void testGetAccountByIdSuccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockPassengerAccount));

        AccountResponse response = accountService.getAccountById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getFullName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account ID not found")
    void testGetAccountByIdNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(999L));
    }

    @Test
    @DisplayName("Should allow user to update their own profile")
    void testUpdateProfileSuccessSelf() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .phoneNumber("0779998877")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockPassengerAccount));
        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockPassengerAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(mockPassengerAccount);

        AccountResponse response = accountService.updateProfile(1L, request, "john@example.com");

        assertNotNull(response);
        assertEquals("John Updated", mockPassengerAccount.getFullName());
        assertEquals("0779998877", mockPassengerAccount.getPhoneNumber());
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when non-admin attempts to update another profile")
    void testUpdateProfileUnauthorized() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Hacked Name")
                .phoneNumber("0770000000")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockPassengerAccount));
        when(accountRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(mockDriverAccount));

        assertThrows(UnauthorizedAccessException.class,
                () -> accountService.updateProfile(1L, request, "jane@example.com"));
    }

    @Test
    @DisplayName("Should update account status successfully")
    void testUpdateStatusSuccess() {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockPassengerAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(mockPassengerAccount);

        AccountResponse response = accountService.updateStatus(1L, request);

        assertNotNull(response);
        assertEquals(AccountStatus.SUSPENDED, mockPassengerAccount.getStatus());
    }
}
