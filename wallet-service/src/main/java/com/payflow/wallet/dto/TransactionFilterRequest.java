package com.payflow.wallet.dto;

import com.payflow.wallet.enums.TransactionStatus;
import com.payflow.wallet.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFilterRequest {
    private TransactionType type;
    private TransactionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
}
