package com.payflow.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletStatementResponse {
    private WalletResponse wallet;
    private List<TransactionResponse> transactions;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private String statementPeriod;
}
