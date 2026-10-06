package com.ridelink.account.dto.response;

import com.ridelink.account.enums.AccountStatus;
import com.ridelink.account.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long id;

    private String email;

    private String fullName;

    private Role role;

    private AccountStatus status;
}
