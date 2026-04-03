package com.payflow.wallet.controller;

import com.payflow.wallet.dto.CreateWalletRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "APIs for wallet creation and management")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet", description = "Creates a new digital wallet for a user with zero balance")
    @ApiResponse(responseCode = "201", description = "Wallet created successfully")
    @ApiResponse(responseCode = "409", description = "Wallet already exists for this user")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet by ID", description = "Retrieves wallet details by wallet ID")
    @ApiResponse(responseCode = "200", description = "Wallet found")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getWallet(walletId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user ID", description = "Retrieves wallet details by the owner's user ID")
    @ApiResponse(responseCode = "200", description = "Wallet found")
    @ApiResponse(responseCode = "404", description = "Wallet not found for this user")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @PutMapping("/{walletId}/freeze")
    @Operation(summary = "Freeze wallet", description = "Freezes a wallet to prevent any transactions")
    @ApiResponse(responseCode = "200", description = "Wallet frozen successfully")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<WalletResponse> freezeWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.freezeWallet(walletId));
    }

    @PutMapping("/{walletId}/unfreeze")
    @Operation(summary = "Unfreeze wallet", description = "Unfreezes a previously frozen wallet")
    @ApiResponse(responseCode = "200", description = "Wallet unfrozen successfully")
    @ApiResponse(responseCode = "400", description = "Wallet is not frozen")
    public ResponseEntity<WalletResponse> unfreezeWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.unfreezeWallet(walletId));
    }
}
