package com.payflow.wallet.service;

import com.payflow.wallet.exception.DailyLimitExceededException;
import com.payflow.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Enforces daily transfer limits to prevent abuse and comply with
 * regulatory requirements. The default daily limit is 100,000 INR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyLimitService {

    private final TransactionRepository transactionRepository;

    @Value("${wallet.daily-transfer-limit:100000}")
    private BigDecimal dailyTransferLimit;

    /**
     * Checks whether a wallet has exceeded its daily transfer limit.
     * The limit is calculated from the start of the current day (midnight).
     *
     * @param walletId the sender wallet ID
     * @param amount   the amount of the new transfer
     * @return true if the transfer is within the daily limit
     * @throws DailyLimitExceededException if the transfer would exceed the daily limit
     */
    public boolean checkDailyLimit(Long walletId, BigDecimal amount) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);

        BigDecimal totalToday = transactionRepository.sumDailyTransferAmount(walletId, startOfDay);
        BigDecimal projectedTotal = totalToday.add(amount);

        if (projectedTotal.compareTo(dailyTransferLimit) > 0) {
            log.warn("Daily limit exceeded for walletId={}: todayTotal={}, requested={}, limit={}",
                    walletId, totalToday, amount, dailyTransferLimit);
            throw new DailyLimitExceededException(
                    String.format("Daily transfer limit of %s exceeded. Today's total: %s, Requested: %s",
                            dailyTransferLimit, totalToday, amount));
        }

        return true;
    }
}
