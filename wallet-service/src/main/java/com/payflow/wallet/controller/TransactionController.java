package com.payflow.wallet.controller;

import com.payflow.wallet.dto.*;
import com.payflow.wallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for money transfers and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/wallets/{walletId}/add-money")
    @Operation(summary = "Add money to wallet", description = "Credits money into a wallet (simulates top-up)")
    @ApiResponse(responseCode = "200", description = "Money added successfully")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    @ApiResponse(responseCode = "403", description = "Wallet is frozen")
    public ResponseEntity<TransactionResponse> addMoney(@PathVariable Long walletId,
                                                         @Valid @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(transactionService.addMoney(walletId, request));
    }

    @PostMapping("/wallets/{walletId}/send-money")
    @Operation(summary = "Send money to another wallet", description = "Transfers money between two wallets")
    @ApiResponse(responseCode = "200", description = "Money sent successfully")
    @ApiResponse(responseCode = "400", description = "Insufficient balance or invalid request")
    @ApiResponse(responseCode = "403", description = "Wallet is frozen")
    @ApiResponse(responseCode = "429", description = "Daily transfer limit exceeded")
    public ResponseEntity<TransactionResponse> sendMoney(@PathVariable Long walletId,
                                                          @Valid @RequestBody SendMoneyRequest request) {
        return ResponseEntity.ok(transactionService.sendMoney(walletId, request));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details", description = "Retrieves details of a specific transaction")
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "400", description = "Transaction not found")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/wallets/{walletId}/history")
    @Operation(summary = "Get transaction history", description = "Retrieves paginated transaction history for a wallet")
    @ApiResponse(responseCode = "200", description = "Transaction history retrieved")
    public ResponseEntity<PagedResponse<TransactionResponse>> getTransactionHistory(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(transactionService.getTransactionHistory(walletId, pageable));
    }

    @GetMapping("/wallets/{walletId}/statement")
    @Operation(summary = "Get wallet statement", description = "Generates a wallet statement for a given date range")
    @ApiResponse(responseCode = "200", description = "Statement generated successfully")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<WalletStatementResponse> getWalletStatement(
            @PathVariable Long walletId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(transactionService.getWalletStatement(walletId, from, to));
    }

    @PostMapping("/{transactionId}/reverse")
    @Operation(summary = "Reverse a transaction", description = "Reverses a completed transaction and restores balances")
    @ApiResponse(responseCode = "200", description = "Transaction reversed successfully")
    @ApiResponse(responseCode = "400", description = "Transaction cannot be reversed")
    public ResponseEntity<TransactionResponse> reverseTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.reverseTransaction(transactionId));
    }
}
