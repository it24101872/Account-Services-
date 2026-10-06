package com.ridelink.account.dto.request;

import com.ridelink.account.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest {

    @NotNull(message = "Status is required (ACTIVE, INACTIVE, SUSPENDED)")
    private AccountStatus status;
}
