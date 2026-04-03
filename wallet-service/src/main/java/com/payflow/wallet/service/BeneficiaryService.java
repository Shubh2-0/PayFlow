package com.payflow.wallet.service;

import com.payflow.wallet.dto.BeneficiaryRequest;
import com.payflow.wallet.dto.BeneficiaryResponse;
import com.payflow.wallet.entity.BeneficiaryWallet;
import com.payflow.wallet.exception.DuplicateWalletException;
import com.payflow.wallet.exception.WalletNotFoundException;
import com.payflow.wallet.repository.BeneficiaryWalletRepository;
import com.payflow.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages saved beneficiaries for quick peer-to-peer transfers.
 * Users can save frequently used wallets with a nickname for easy access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryWalletRepository beneficiaryWalletRepository;
    private final WalletRepository walletRepository;

    /**
     * Adds a new beneficiary to a wallet's saved list.
     * Both the owner wallet and beneficiary wallet must exist.
     * Duplicate beneficiaries (same owner + beneficiary pair) are rejected.
     *
     * @param walletId the owner wallet ID
     * @param request  the beneficiary details
     * @return the saved beneficiary details
     * @throws WalletNotFoundException if either wallet does not exist
     * @throws DuplicateWalletException if the beneficiary is already saved
     */
    @Transactional
    public BeneficiaryResponse addBeneficiary(Long walletId, BeneficiaryRequest request) {
        // Validate owner wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException("Owner wallet not found with id: " + walletId);
        }

        // Validate beneficiary wallet exists
        if (!walletRepository.existsById(request.getBeneficiaryWalletId())) {
            throw new WalletNotFoundException("Beneficiary wallet not found with id: " + request.getBeneficiaryWalletId());
        }

        // Check for duplicate
        if (beneficiaryWalletRepository.existsByOwnerWalletIdAndBeneficiaryWalletId(
                walletId, request.getBeneficiaryWalletId())) {
            throw new DuplicateWalletException("Beneficiary already exists for this wallet");
        }

        BeneficiaryWallet beneficiary = BeneficiaryWallet.builder()
                .ownerWalletId(walletId)
                .beneficiaryWalletId(request.getBeneficiaryWalletId())
                .nickname(request.getNickname())
                .build();

        beneficiary = beneficiaryWalletRepository.save(beneficiary);
        log.info("Added beneficiary walletId={} for ownerWalletId={} with nickname='{}'",
                request.getBeneficiaryWalletId(), walletId, request.getNickname());
        return mapToResponse(beneficiary);
    }

    /**
     * Retrieves all saved beneficiaries for a wallet.
     *
     * @param walletId the owner wallet ID
     * @return list of saved beneficiaries
     */
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(Long walletId) {
        return beneficiaryWalletRepository.findByOwnerWalletId(walletId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Removes a saved beneficiary from a wallet's list.
     *
     * @param walletId      the owner wallet ID
     * @param beneficiaryId the beneficiary record ID to remove
     * @throws WalletNotFoundException if the beneficiary record does not exist or does not belong to the wallet
     */
    @Transactional
    public void removeBeneficiary(Long walletId, Long beneficiaryId) {
        BeneficiaryWallet beneficiary = beneficiaryWalletRepository.findById(beneficiaryId)
                .orElseThrow(() -> new WalletNotFoundException("Beneficiary not found with id: " + beneficiaryId));

        if (!beneficiary.getOwnerWalletId().equals(walletId)) {
            throw new WalletNotFoundException("Beneficiary does not belong to wallet: " + walletId);
        }

        beneficiaryWalletRepository.delete(beneficiary);
        log.info("Removed beneficiary id={} from walletId={}", beneficiaryId, walletId);
    }

    private BeneficiaryResponse mapToResponse(BeneficiaryWallet beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .beneficiaryWalletId(beneficiary.getBeneficiaryWalletId())
                .nickname(beneficiary.getNickname())
                .createdAt(beneficiary.getCreatedAt())
                .build();
    }
}
