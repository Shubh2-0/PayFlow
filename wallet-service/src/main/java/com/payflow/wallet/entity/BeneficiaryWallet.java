package com.payflow.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiary_wallets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ownerWalletId", "beneficiaryWalletId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerWalletId;

    @Column(nullable = false)
    private Long beneficiaryWalletId;

    @Column(nullable = false)
    private String nickname;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
