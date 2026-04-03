package com.payflow.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.wallet.dto.*;
import com.payflow.wallet.enums.TransactionStatus;
import com.payflow.wallet.enums.TransactionType;
import com.payflow.wallet.enums.WalletStatus;
import com.payflow.wallet.exception.GlobalExceptionHandler;
import com.payflow.wallet.exception.InsufficientBalanceException;
import com.payflow.wallet.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionResponse sampleTransactionResponse(TransactionType type) {
        return TransactionResponse.builder()
                .id(1L)
                .transactionRef("ref-123")
                .type(type)
                .amount(new BigDecimal("1000.0000"))
                .balanceBefore(new BigDecimal("5000.0000"))
                .balanceAfter(type == TransactionType.CREDIT
                        ? new BigDecimal("6000.0000")
                        : new BigDecimal("4000.0000"))
                .status(TransactionStatus.COMPLETED)
                .description("Test transaction")
                .senderWalletId(type == TransactionType.TRANSFER ? 1L : null)
                .receiverWalletId(type == TransactionType.TRANSFER ? 2L : 1L)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/transactions/wallets/{id}/add-money should return 200")
    void test_addMoney_returns200() throws Exception {
        AddMoneyRequest request = AddMoneyRequest.builder()
                .amount(new BigDecimal("1000"))
                .description("Top up")
                .idempotencyKey("idem-1")
                .build();

        when(transactionService.addMoney(eq(1L), any())).thenReturn(sampleTransactionResponse(TransactionType.CREDIT));

        mockMvc.perform(post("/api/transactions/wallets/1/add-money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/transactions/wallets/{id}/send-money should return 200")
    void test_sendMoney_returns200() throws Exception {
        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(2L)
                .amount(new BigDecimal("1000"))
                .description("Payment")
                .idempotencyKey("idem-2")
                .build();

        when(transactionService.sendMoney(eq(1L), any())).thenReturn(sampleTransactionResponse(TransactionType.TRANSFER));

        mockMvc.perform(post("/api/transactions/wallets/1/send-money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.senderWalletId").value(1))
                .andExpect(jsonPath("$.receiverWalletId").value(2));
    }

    @Test
    @DisplayName("POST /api/transactions/wallets/{id}/send-money should return 400 on insufficient balance")
    void test_sendMoney_insufficientBalance_returns400() throws Exception {
        SendMoneyRequest request = SendMoneyRequest.builder()
                .receiverWalletId(2L)
                .amount(new BigDecimal("99999"))
                .idempotencyKey("idem-3")
                .build();

        when(transactionService.sendMoney(eq(1L), any()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/api/transactions/wallets/1/send-money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance"));
    }

    @Test
    @DisplayName("GET /api/transactions/wallets/{id}/history should return paged results")
    void test_getTransactionHistory_returnsPaged() throws Exception {
        PagedResponse<TransactionResponse> pagedResponse = PagedResponse.<TransactionResponse>builder()
                .content(List.of(sampleTransactionResponse(TransactionType.CREDIT)))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(transactionService.getTransactionHistory(eq(1L), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/transactions/wallets/1/history")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /api/transactions/wallets/{id}/statement should return statement")
    void test_getStatement_returns200() throws Exception {
        WalletStatementResponse statement = WalletStatementResponse.builder()
                .wallet(WalletResponse.builder()
                        .id(1L)
                        .userId("user-1")
                        .balance(new BigDecimal("5000"))
                        .currency("INR")
                        .status(WalletStatus.ACTIVE)
                        .build())
                .transactions(List.of(sampleTransactionResponse(TransactionType.CREDIT)))
                .totalCredits(new BigDecimal("1000"))
                .totalDebits(BigDecimal.ZERO)
                .statementPeriod("2026-03-01 to 2026-04-01")
                .build();

        when(transactionService.getWalletStatement(eq(1L), any(), any())).thenReturn(statement);

        mockMvc.perform(get("/api/transactions/wallets/1/statement")
                        .param("from", "2026-03-01")
                        .param("to", "2026-04-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.id").value(1))
                .andExpect(jsonPath("$.totalCredits").value(1000))
                .andExpect(jsonPath("$.statementPeriod").value("2026-03-01 to 2026-04-01"));
    }
}
