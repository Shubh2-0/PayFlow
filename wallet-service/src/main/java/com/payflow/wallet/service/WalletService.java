package com.payflow.wallet.service;

import com.payflow.wallet.dto.CreateWalletRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.enums.WalletStatus;
import com.payflow.wallet.exception.DuplicateWalletException;
import com.payflow.wallet.exception.InvalidTransactionException;
import com.payflow.wallet.exception.WalletNotFoundException;
import com.payflow.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service responsible for wallet lifecycle management including creation,
 * retrieval, and status changes (freeze/unfreeze).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;

    /**
     * Creates a new wallet for a user with zero balance.
     * Each user can only have one wallet - duplicates are rejected.
     *
     * @param request the wallet creation request containing userId and currency
     * @return the created wallet details
     * @throws DuplicateWalletException if a wallet already exists for the given userId
     */
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        if (walletRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateWalletException("Wallet already exists for user: " + request.getUserId());
        }

        Wallet wallet = Wallet.builder()
                .userId(request.getUserId())
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(WalletStatus.ACTIVE)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Created wallet id={} for userId={}", wallet.getId(), wallet.getUserId());
        return mapToResponse(wallet);
    }

    /**
     * Retrieves a wallet by its unique identifier.
     *
     * @param walletId the wallet ID
     * @return the wallet details
     * @throws WalletNotFoundException if no wallet exists with the given ID
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
        return mapToResponse(wallet);
    }

    /**
     * Retrieves a wallet by the owner's user ID.
     *
     * @param userId the user ID from the auth service
     * @return the wallet details
     * @throws WalletNotFoundException if no wallet exists for the given user
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return mapToResponse(wallet);
    }

    /**
     * Freezes a wallet, preventing any transactions from being processed.
     * Only ACTIVE wallets can be frozen.
     *
     * @param walletId the wallet ID to freeze
     * @return the updated wallet details
     * @throws WalletNotFoundException if the wallet does not exist
     * @throws InvalidTransactionException if the wallet is already CLOSED
     */
    @Transactional
    public WalletResponse freezeWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new InvalidTransactionException("Cannot freeze a closed wallet");
        }

        wallet.setStatus(WalletStatus.FROZEN);
        wallet = walletRepository.save(wallet);
        log.info("Wallet id={} has been frozen", walletId);
        return mapToResponse(wallet);
    }

    /**
     * Unfreezes a previously frozen wallet, allowing transactions to resume.
     * Only FROZEN wallets can be unfrozen.
     *
     * @param walletId the wallet ID to unfreeze
     * @return the updated wallet details
     * @throws WalletNotFoundException if the wallet does not exist
     * @throws InvalidTransactionException if the wallet is not in FROZEN status
     */
    @Transactional
    public WalletResponse unfreezeWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (wallet.getStatus() != WalletStatus.FROZEN) {
            throw new InvalidTransactionException("Wallet is not frozen, current status: " + wallet.getStatus());
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        wallet = walletRepository.save(wallet);
        log.info("Wallet id={} has been unfrozen", walletId);
        return mapToResponse(wallet);
    }

    /**
     * Maps a Wallet entity to a WalletResponse DTO.
     */
    public WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
