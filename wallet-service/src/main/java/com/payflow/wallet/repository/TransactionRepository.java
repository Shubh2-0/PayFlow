package com.payflow.wallet.repository;

import com.payflow.wallet.entity.Transaction;
import com.payflow.wallet.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySenderWalletIdOrReceiverWalletId(Long senderWalletId, Long receiverWalletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.senderWalletId = :walletId OR t.receiverWalletId = :walletId) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderWalletId = :walletId " +
            "AND t.status = :status AND t.createdAt > :after")
    long countBySenderWalletIdAndStatusAndCreatedAtAfter(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status,
            @Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderWalletId = :walletId " +
            "AND t.type = 'TRANSFER' AND t.status = 'COMPLETED' AND t.createdAt > :after")
    java.math.BigDecimal sumDailyTransferAmount(
            @Param("walletId") Long walletId,
            @Param("after") LocalDateTime after);
}
