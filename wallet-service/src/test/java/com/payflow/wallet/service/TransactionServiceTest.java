package com.payflow.wallet.service;

import com.payflow.wallet.dto.*;
import com.payflow.wallet.entity.Transaction;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.enums.TransactionStatus;
import com.payflow.wallet.enums.TransactionType;
import com.payflow.wallet.enums.WalletStatus;
import com.payflow.wallet.event.TransactionEventPublisher;
import com.payflow.wallet.exception.InsufficientBalanceException;
import com.payflow.wallet.exception.InvalidTransactionException;
import com.payflow.wallet.exception.WalletFrozenException;
import com.payflow.wallet.repository.TransactionRepository;
import com.payflow.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private DailyLimitService dailyLimitService;

    @Mock
    private TransactionEventPublisher eventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    private Wallet createActiveWallet(Long id, String userId, BigDecimal balance) {
        return Wallet.builder()
                .id(id)
                .userId(userId)
                .balance(balance)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should add money to wallet successfully")
    void test_addMoney_success() {
        Wallet wallet = createActiveWallet(1L, "user-1", new BigDecimal("1000.0000"));

        AddMoneyRequest request = AddMoneyRequest.builder()
                .amount(new BigDecimal("500.0000"))
                .description("Test top-up")
                .idempotencyKey("idem-1")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionRef("ref-123")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("500.0000"))
                .balanceBefore(new BigDecimal("1000.0000"))
                .balanceAfter(new BigDecimal("1500.0000"))
                .status(TransactionStatus.COMPLETED)
                .description("Test top-up")
                .receiverWalletId(1L)
                .idempotencyKey("idem-1")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.addMoney(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publish(any(Transaction.class));
    }

    @Test
    @DisplayName("Should return existing transaction for idempotent add money request")
    void test_addMoney_idempotent_returnsSameTransaction() {
        Transaction existingTransaction = Transaction.builder()
                .id(1L)
                .transactionRef("ref-123")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("500.0000"))
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-1")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existingTransaction));

        AddMoneyRequest request = AddMoneyRequest.builder()
                .amount(new BigDecimal("500.0000"))
                .idempotencyKey("idem-1")
                .build();

        TransactionResponse response = transactionService.addMoney(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        verify(walletRepository, never()).findByIdWithLock(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw WalletFrozenException when adding money to frozen wallet")
    void test_addMoney_walletFrozen_throwsException() {
        Wallet frozenWallet = Wallet.builder()
                .id(1L)
                .userId("user-1")
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.FROZEN)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(frozenWallet));

        AddMoneyRequest request = AddMoneyRequest.builder()
                .amount(new BigDecimal("500"))
                .idempotencyKey("idem-1")
                .build();

        assertThatThrownBy(() -> transactionService.addMoney(1L, request))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    @DisplayName("Should send money between wallets successfully")
    void test_sendMoney_success() {
        Wallet sender = createActiveWallet(1L, "user-1", new BigDecimal("5000.0000"));
        Wallet receiver = createActiveWallet(2L, "user-2", new BigDecimal("1000.0000"));

        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(2L)
                .amount(new BigDecimal("2000.0000"))
                .description("Payment")
                .idempotencyKey("idem-2")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionRef("ref-456")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("2000.0000"))
                .balanceBefore(new BigDecimal("5000.0000"))
                .balanceAfter(new BigDecimal("3000.0000"))
                .status(TransactionStatus.COMPLETED)
                .senderWalletId(1L)
                .receiverWalletId(2L)
                .idempotencyKey("idem-2")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(dailyLimitService.checkDailyLimit(eq(1L), any())).thenReturn(true);
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByIdWithLock(2L)).thenReturn(Optional.of(receiver));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.sendMoney(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(response.getSenderWalletId()).isEqualTo(1L);
        assertThat(response.getReceiverWalletId()).isEqualTo(2L);

        // Verify both wallets were saved (balance updated)
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(eventPublisher).publish(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when sender has insufficient funds")
    void test_sendMoney_insufficientBalance_throwsException() {
        Wallet sender = createActiveWallet(1L, "user-1", new BigDecimal("100.0000"));
        Wallet receiver = createActiveWallet(2L, "user-2", new BigDecimal("1000.0000"));

        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(2L)
                .amount(new BigDecimal("500.0000"))
                .idempotencyKey("idem-3")
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(dailyLimitService.checkDailyLimit(eq(1L), any())).thenReturn(true);
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByIdWithLock(2L)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transactionService.sendMoney(1L, request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when sending to same wallet")
    void test_sendMoney_sameWallet_throwsException() {
        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(1L)
                .amount(new BigDecimal("100"))
                .idempotencyKey("idem-4")
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.sendMoney(1L, request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("same wallet");
    }

    @Test
    @DisplayName("Should throw WalletFrozenException when sender wallet is frozen")
    void test_sendMoney_walletFrozen_throwsException() {
        Wallet frozenSender = Wallet.builder()
                .id(1L)
                .userId("user-1")
                .balance(new BigDecimal("5000"))
                .status(WalletStatus.FROZEN)
                .build();
        Wallet receiver = createActiveWallet(2L, "user-2", new BigDecimal("1000"));

        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(2L)
                .amount(new BigDecimal("100"))
                .idempotencyKey("idem-5")
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-5")).thenReturn(Optional.empty());
        when(dailyLimitService.checkDailyLimit(eq(1L), any())).thenReturn(true);
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(frozenSender));
        when(walletRepository.findByIdWithLock(2L)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transactionService.sendMoney(1L, request))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    @DisplayName("Should return existing transaction for idempotent send money request")
    void test_sendMoney_idempotent_returnsSameTransaction() {
        Transaction existing = Transaction.builder()
                .id(1L)
                .transactionRef("ref-existing")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("500"))
                .status(TransactionStatus.COMPLETED)
                .senderWalletId(1L)
                .receiverWalletId(2L)
                .idempotencyKey("idem-dup")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));

        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(2L)
                .amount(new BigDecimal("500"))
                .idempotencyKey("idem-dup")
                .build();

        TransactionResponse response = transactionService.sendMoney(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        verify(walletRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Should reverse a completed transaction successfully")
    void test_reverseTransaction_success() {
        Transaction original = Transaction.builder()
                .id(1L)
                .transactionRef("ref-orig")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("500.0000"))
                .balanceBefore(new BigDecimal("1000.0000"))
                .balanceAfter(new BigDecimal("1500.0000"))
                .status(TransactionStatus.COMPLETED)
                .receiverWalletId(1L)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        Wallet wallet = createActiveWallet(1L, "user-1", new BigDecimal("1500.0000"));

        Transaction reversalSaved = Transaction.builder()
                .id(2L)
                .transactionRef("ref-reversal")
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal("500.0000"))
                .balanceBefore(new BigDecimal("1500.0000"))
                .balanceAfter(new BigDecimal("1000.0000"))
                .status(TransactionStatus.COMPLETED)
                .senderWalletId(1L)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(original));
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() != null && t.getId().equals(1L)) return t; // original update
            return reversalSaved; // reversal save
        });

        TransactionResponse response = transactionService.reverseTransaction(1L);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(original.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        verify(eventPublisher).publish(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when reversing an already reversed transaction")
    void test_reverseTransaction_alreadyReversed_throwsException() {
        Transaction reversed = Transaction.builder()
                .id(1L)
                .transactionRef("ref-rev")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("500"))
                .status(TransactionStatus.REVERSED)
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(reversed));

        assertThatThrownBy(() -> transactionService.reverseTransaction(1L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("Should generate wallet statement with correct totals")
    void test_getWalletStatement_success() {
        Wallet wallet = createActiveWallet(1L, "user-1", new BigDecimal("5000.0000"));

        Transaction credit = Transaction.builder()
                .id(1L)
                .transactionRef("ref-1")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("3000.0000"))
                .status(TransactionStatus.COMPLETED)
                .receiverWalletId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction transfer = Transaction.builder()
                .id(2L)
                .transactionRef("ref-2")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("1000.0000"))
                .status(TransactionStatus.COMPLETED)
                .senderWalletId(1L)
                .receiverWalletId(2L)
                .createdAt(LocalDateTime.now())
                .build();

        WalletResponse walletResponse = WalletResponse.builder()
                .id(1L)
                .userId("user-1")
                .balance(new BigDecimal("5000.0000"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findTransactionsByWalletIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of(credit, transfer));
        when(walletService.mapToResponse(wallet)).thenReturn(walletResponse);

        WalletStatementResponse statement = transactionService.getWalletStatement(
                1L, LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(statement).isNotNull();
        assertThat(statement.getTransactions()).hasSize(2);
        assertThat(statement.getTotalCredits()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(statement.getTotalDebits()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(statement.getStatementPeriod()).contains("to");
    }
}
