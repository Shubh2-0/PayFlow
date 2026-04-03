package com.payflow.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @Builder.Default
    private String currency = "INR";
}
