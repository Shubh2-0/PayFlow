package com.payflow.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryRequest {

    @NotNull(message = "Beneficiary wallet ID is required")
    private Long beneficiaryWalletId;

    @NotBlank(message = "Nickname is required")
    private String nickname;
}
