package com.ridelink.account.service;

import com.ridelink.account.dto.request.LoginRequest;
import com.ridelink.account.dto.request.RegisterRequest;
import com.ridelink.account.dto.request.UpdateProfileRequest;
import com.ridelink.account.dto.request.UpdateStatusRequest;
import com.ridelink.account.dto.response.AccountResponse;
import com.ridelink.account.dto.response.AuthResponse;

public interface AccountService {

    AccountResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AccountResponse getAccountById(Long id);

    AccountResponse getAccountByEmail(String email);

    AccountResponse updateProfile(Long id, UpdateProfileRequest request, String authenticatedEmail);

    AccountResponse updateStatus(Long id, UpdateStatusRequest request);
}
