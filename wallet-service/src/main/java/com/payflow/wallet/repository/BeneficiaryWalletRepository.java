package com.payflow.wallet.repository;

import com.payflow.wallet.entity.BeneficiaryWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryWalletRepository extends JpaRepository<BeneficiaryWallet, Long> {

    List<BeneficiaryWallet> findByOwnerWalletId(Long ownerWalletId);

    boolean existsByOwnerWalletIdAndBeneficiaryWalletId(Long ownerWalletId, Long beneficiaryWalletId);
}
