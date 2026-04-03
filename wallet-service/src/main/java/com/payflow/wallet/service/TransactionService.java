package com.payflow.wallet.service;

import com.payflow.wallet.dto.*;
import com.payflow.wallet.entity.Transaction;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.enums.TransactionStatus;
import com.payflow.wallet.enums.TransactionType;
import com.payflow.wallet.enums.WalletStatus;
import com.payflow.wallet.event.TransactionEventPublisher;
import com.payflow.wallet.exception.*;
import com.payflow.wallet.repository.TransactionRepository;
import com.payflow.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core transaction processing service. Handles money additions, peer-to-peer transfers,
 * transaction reversals, and statement generation.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Pessimistic write locks on wallets during balance mutations to ensure consistency</li>
 *   <li>Idempotency keys prevent duplicate transactions from retried requests</li>
 *   <li>Wallets are always locked in ascending ID order during transfers to prevent deadlocks</li>
 *   <li>All monetary values use BigDecimal to avoid floating-point precision issues</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final DailyLimitService dailyLimitService;
    private final TransactionEventPublisher eventPublisher;

    /**
     * Adds money (credit) to a wallet. This simulates a top-up from an external payment source.
     *
     * <p>Flow:
     * 1. Check idempotency - if a transaction with the same key exists, return it immediately
     * 2. Validate the wallet exists and is ACTIVE
     * 3. Acquire a pessimistic write lock on the wallet
     * 4. Create a CREDIT transaction, update the wallet balance
     * 5. Publish a transaction event to RabbitMQ for downstream processing
     *
     * @param walletId the target wallet ID
     * @param request  the add money request containing amount, description, and idempotency key
     * @return the completed transaction details
     * @throws WalletNotFoundException if the wallet does not exist
     * @throws WalletFrozenException   if the wallet is frozen or closed
     */
    @Transactional
    public TransactionResponse addMoney(Long walletId, AddMoneyRequest request) {
        // Idempotency check
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key={}, returning existing transaction id={}",
                    request.getIdempotencyKey(), existing.get().getId());
            return mapToResponse(existing.get());
        }

        // Acquire pessimistic lock and validate
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        validateWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .type(TransactionType.CREDIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Wallet top-up")
                .receiverWalletId(walletId)
                .idempotencyKey(request.getIdempotencyKey())
                .completedAt(LocalDateTime.now())
                .build();

        // Update wallet balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);
        transaction = transactionRepository.save(transaction);

        log.info("Added {} to walletId={}, newBalance={}", request.getAmount(), walletId, balanceAfter);

        // Publish event asynchronously
        eventPublisher.publish(transaction);

        return mapToResponse(transaction);
    }

    /**
     * Transfers money between two wallets. Implements peer-to-peer transfer with
     * full ACID guarantees.
     *
     * <p>Flow:
     * 1. Check idempotency
     * 2. Validate sender != receiver
     * 3. Check daily transfer limit
     * 4. Acquire pessimistic write locks on BOTH wallets (in ascending ID order to prevent deadlocks)
     * 5. Validate both wallets are ACTIVE and sender has sufficient balance
     * 6. Create a TRANSFER transaction, debit sender, credit receiver
     * 7. Publish transaction event
     *
     * @param senderWalletId the sender's wallet ID
     * @param request        the send money request containing receiver, amount, description, and idempotency key
     * @return the completed transaction details
     * @throws InvalidTransactionException   if sender and receiver are the same
     * @throws WalletNotFoundException       if either wallet does not exist
     * @throws WalletFrozenException         if either wallet is frozen or closed
     * @throws InsufficientBalanceException  if the sender has insufficient balance
     * @throws DailyLimitExceededException   if the daily transfer limit is exceeded
     */
    @Transactional
    public TransactionResponse sendMoney(Long senderWalletId, SendMoneyRequest request) {
        // Idempotency check
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key={}, returning existing transaction id={}",
                    request.getIdempotencyKey(), existing.get().getId());
            return mapToResponse(existing.get());
        }

        // Validate sender != receiver
        if (senderWalletId.equals(request.getReceiverWalletId())) {
            throw new InvalidTransactionException("Cannot transfer money to the same wallet");
        }

        // Check daily limit
        dailyLimitService.checkDailyLimit(senderWalletId, request.getAmount());

        // Lock wallets in ascending ID order to prevent deadlocks
        Long firstLockId = Math.min(senderWalletId, request.getReceiverWalletId());
        Long secondLockId = Math.max(senderWalletId, request.getReceiverWalletId());

        Wallet firstWallet = walletRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + firstLockId));
        Wallet secondWallet = walletRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + secondLockId));

        // Assign sender and receiver correctly after locking
        Wallet senderWallet = senderWalletId.equals(firstLockId) ? firstWallet : secondWallet;
        Wallet receiverWallet = senderWalletId.equals(firstLockId) ? secondWallet : firstWallet;

        // Validate both wallets are active
        validateWalletActive(senderWallet);
        validateWalletActive(receiverWallet);

        // Validate sufficient balance
        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %s, Requested: %s",
                            senderWallet.getBalance(), request.getAmount()));
        }

        BigDecimal senderBalanceBefore = senderWallet.getBalance();
        BigDecimal senderBalanceAfter = senderBalanceBefore.subtract(request.getAmount());
        BigDecimal receiverBalanceBefore = receiverWallet.getBalance();
        BigDecimal receiverBalanceAfter = receiverBalanceBefore.add(request.getAmount());

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderBalanceAfter)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Wallet transfer")
                .senderWalletId(senderWalletId)
                .receiverWalletId(request.getReceiverWalletId())
                .idempotencyKey(request.getIdempotencyKey())
                .completedAt(LocalDateTime.now())
                .build();

        // Update balances
        senderWallet.setBalance(senderBalanceAfter);
        receiverWallet.setBalance(receiverBalanceAfter);
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);
        transaction = transactionRepository.save(transaction);

        log.info("Transfer of {} from walletId={} to walletId={} completed. Sender balance: {}, Receiver balance: {}",
                request.getAmount(), senderWalletId, request.getReceiverWalletId(),
                senderBalanceAfter, receiverBalanceAfter);

        // Publish event
        eventPublisher.publish(transaction);

        return mapToResponse(transaction);
    }

    /**
     * Retrieves a single transaction by its ID.
     *
     * @param transactionId the transaction ID
     * @return the transaction details
     * @throws InvalidTransactionException if the transaction does not exist
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found with id: " + transactionId));
        return mapToResponse(transaction);
    }

    /**
     * Retrieves paginated transaction history for a wallet.
     * Includes all transactions where the wallet is either the sender or receiver.
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters
     * @return paged list of transactions
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable) {
        Page<Transaction> page = transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(walletId, walletId, pageable);

        List<TransactionResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Generates a wallet statement for a given date range.
     * Includes wallet details, filtered transactions, and aggregated totals.
     *
     * @param walletId the wallet ID
     * @param from     start date (inclusive)
     * @param to       end date (inclusive)
     * @return the wallet statement with transactions and totals
     * @throws WalletNotFoundException if the wallet does not exist
     */
    @Transactional(readOnly = true)
    public WalletStatementResponse getWalletStatement(Long walletId, LocalDate from, LocalDate to) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findTransactionsByWalletIdAndDateRange(walletId, startDateTime, endDateTime);

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // Calculate totals relative to this wallet
        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> (t.getType() == TransactionType.CREDIT && walletId.equals(t.getReceiverWalletId()))
                        || (t.getType() == TransactionType.TRANSFER && walletId.equals(t.getReceiverWalletId())))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> (t.getType() == TransactionType.DEBIT && walletId.equals(t.getSenderWalletId()))
                        || (t.getType() == TransactionType.TRANSFER && walletId.equals(t.getSenderWalletId())))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String period = from.format(DateTimeFormatter.ISO_LOCAL_DATE) + " to " +
                to.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return WalletStatementResponse.builder()
                .wallet(walletService.mapToResponse(wallet))
                .transactions(transactionResponses)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .statementPeriod(period)
                .build();
    }

    /**
     * Reverses a completed transaction. Creates a new compensating transaction
     * and updates wallet balances accordingly.
     *
     * <p>Only COMPLETED transactions can be reversed. Already REVERSED or FAILED
     * transactions cannot be reversed again.
     *
     * @param transactionId the ID of the transaction to reverse
     * @return the reversal transaction details
     * @throws InvalidTransactionException if the transaction does not exist, is not completed, or is already reversed
     */
    @Transactional
    public TransactionResponse reverseTransaction(Long transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found with id: " + transactionId));

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new InvalidTransactionException(
                    "Only COMPLETED transactions can be reversed. Current status: " + original.getStatus());
        }

        Transaction reversal;

        if (original.getType() == TransactionType.CREDIT) {
            // Reverse a credit: debit the receiver wallet
            Wallet wallet = walletRepository.findByIdWithLock(original.getReceiverWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(
                            "Wallet not found with id: " + original.getReceiverWalletId()));

            if (wallet.getBalance().compareTo(original.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance to reverse the credit");
            }

            BigDecimal balanceBefore = wallet.getBalance();
            BigDecimal balanceAfter = balanceBefore.subtract(original.getAmount());
            wallet.setBalance(balanceAfter);
            walletRepository.save(wallet);

            reversal = Transaction.builder()
                    .transactionRef(UUID.randomUUID().toString())
                    .type(TransactionType.DEBIT)
                    .amount(original.getAmount())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .status(TransactionStatus.COMPLETED)
                    .description("Reversal of transaction: " + original.getTransactionRef())
                    .senderWalletId(original.getReceiverWalletId())
                    .completedAt(LocalDateTime.now())
                    .build();

        } else if (original.getType() == TransactionType.TRANSFER) {
            // Reverse a transfer: credit back the sender, debit the receiver
            Long senderId = original.getSenderWalletId();
            Long receiverId = original.getReceiverWalletId();

            // Lock in ascending order
            Long firstLockId = Math.min(senderId, receiverId);
            Long secondLockId = Math.max(senderId, receiverId);

            Wallet firstWallet = walletRepository.findByIdWithLock(firstLockId)
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + firstLockId));
            Wallet secondWallet = walletRepository.findByIdWithLock(secondLockId)
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + secondLockId));

            Wallet senderWallet = senderId.equals(firstLockId) ? firstWallet : secondWallet;
            Wallet receiverWallet = senderId.equals(firstLockId) ? secondWallet : firstWallet;

            if (receiverWallet.getBalance().compareTo(original.getAmount()) < 0) {
                throw new InsufficientBalanceException("Receiver has insufficient balance to reverse the transfer");
            }

            BigDecimal senderBalanceBefore = senderWallet.getBalance();
            BigDecimal receiverBalanceBefore = receiverWallet.getBalance();

            senderWallet.setBalance(senderBalanceBefore.add(original.getAmount()));
            receiverWallet.setBalance(receiverBalanceBefore.subtract(original.getAmount()));

            walletRepository.save(senderWallet);
            walletRepository.save(receiverWallet);

            reversal = Transaction.builder()
                    .transactionRef(UUID.randomUUID().toString())
                    .type(TransactionType.TRANSFER)
                    .amount(original.getAmount())
                    .balanceBefore(receiverBalanceBefore)
                    .balanceAfter(receiverBalanceBefore.subtract(original.getAmount()))
                    .status(TransactionStatus.COMPLETED)
                    .description("Reversal of transaction: " + original.getTransactionRef())
                    .senderWalletId(receiverId)
                    .receiverWalletId(senderId)
                    .completedAt(LocalDateTime.now())
                    .build();

        } else {
            throw new InvalidTransactionException("Cannot reverse transaction of type: " + original.getType());
        }

        // Mark original as reversed
        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);
        reversal = transactionRepository.save(reversal);

        log.info("Reversed transactionId={}, reversal transactionId={}", transactionId, reversal.getId());

        eventPublisher.publish(reversal);

        return mapToResponse(reversal);
    }

    /**
     * Validates that a wallet is in ACTIVE status.
     *
     * @param wallet the wallet to validate
     * @throws WalletFrozenException if the wallet is FROZEN or CLOSED
     */
    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Wallet id=" + wallet.getId() + " is " + wallet.getStatus()
                    + ". Transactions are not allowed.");
        }
    }

    /**
     * Maps a Transaction entity to a TransactionResponse DTO.
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionRef(transaction.getTransactionRef())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .senderWalletId(transaction.getSenderWalletId())
                .receiverWalletId(transaction.getReceiverWalletId())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
