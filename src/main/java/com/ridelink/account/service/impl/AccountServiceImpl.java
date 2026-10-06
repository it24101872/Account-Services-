package com.ridelink.account.service.impl;

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
import com.ridelink.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AccountResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered: " + request.getEmail());
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.PASSENGER;

        Account account = Account.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber().trim())
                .role(assignedRole)
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Registered new account ID: {}, email: {}, role: {}", savedAccount.getId(), savedAccount.getEmail(), savedAccount.getRole());

        return mapToAccountResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Account is " + account.getStatus().name().toLowerCase() + ". Please contact support.");
        }

        String token = jwtTokenProvider.generateToken(
                account.getId(),
                account.getEmail(),
                account.getRole().name(),
                account.getFullName()
        );

        log.info("Account logged in successfully ID: {}, email: {}", account.getId(), account.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .role(account.getRole())
                .status(account.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
        return mapToAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByEmail(String email) {
        Account account = accountRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with email: " + email));
        return mapToAccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse updateProfile(Long id, UpdateProfileRequest request, String authenticatedEmail) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));

        Account authenticatedUser = accountRepository.findByEmail(authenticatedEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        boolean isSelf = account.getId().equals(authenticatedUser.getId());
        boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;

        if (!isSelf && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to update another user's profile");
        }

        account.setFullName(request.getFullName().trim());
        account.setPhoneNumber(request.getPhoneNumber().trim());

        Account updatedAccount = accountRepository.save(account);
        log.info("Profile updated for account ID: {}", updatedAccount.getId());

        return mapToAccountResponse(updatedAccount);
    }

    @Override
    @Transactional
    public AccountResponse updateStatus(Long id, UpdateStatusRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));

        account.setStatus(request.getStatus());
        Account updatedAccount = accountRepository.save(account);
        log.info("Status updated for account ID: {} to {}", updatedAccount.getId(), request.getStatus());

        return mapToAccountResponse(updatedAccount);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .role(account.getRole())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
