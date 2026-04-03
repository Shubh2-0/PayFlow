package com.payflow.wallet.event;

import com.payflow.wallet.enums.TransactionStatus;
import com.payflow.wallet.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent implements Serializable {
    private Long transactionId;
    private TransactionType type;
    private BigDecimal amount;
    private Long senderWalletId;
    private Long receiverWalletId;
    private TransactionStatus status;
    private LocalDateTime timestamp;
}
