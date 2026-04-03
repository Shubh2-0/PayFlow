package com.payflow.notification.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent implements Serializable {

    private Long transactionId;
    private String transactionRef;
    private String type;
    private String amount;
    private Long senderWalletId;
    private Long receiverWalletId;
    private String status;
    private String description;
    private String timestamp;
}
