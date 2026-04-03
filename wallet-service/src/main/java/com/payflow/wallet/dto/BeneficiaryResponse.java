package com.payflow.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryResponse {
    private Long id;
    private Long beneficiaryWalletId;
    private String nickname;
    private LocalDateTime createdAt;
}
