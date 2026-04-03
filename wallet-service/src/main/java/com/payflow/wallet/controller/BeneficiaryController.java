package com.payflow.wallet.controller;

import com.payflow.wallet.dto.BeneficiaryRequest;
import com.payflow.wallet.dto.BeneficiaryResponse;
import com.payflow.wallet.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets/{walletId}/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiary Management", description = "APIs for managing saved beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    @Operation(summary = "Add beneficiary", description = "Saves a new beneficiary wallet for quick transfers")
    @ApiResponse(responseCode = "201", description = "Beneficiary added successfully")
    @ApiResponse(responseCode = "409", description = "Beneficiary already exists")
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(@PathVariable Long walletId,
                                                               @Valid @RequestBody BeneficiaryRequest request) {
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(walletId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List beneficiaries", description = "Lists all saved beneficiaries for a wallet")
    @ApiResponse(responseCode = "200", description = "Beneficiaries retrieved")
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiaries(@PathVariable Long walletId) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(walletId));
    }

    @DeleteMapping("/{beneficiaryId}")
    @Operation(summary = "Remove beneficiary", description = "Removes a saved beneficiary from the wallet")
    @ApiResponse(responseCode = "204", description = "Beneficiary removed")
    @ApiResponse(responseCode = "404", description = "Beneficiary not found")
    public ResponseEntity<Void> removeBeneficiary(@PathVariable Long walletId,
                                                    @PathVariable Long beneficiaryId) {
        beneficiaryService.removeBeneficiary(walletId, beneficiaryId);
        return ResponseEntity.noContent().build();
    }
}
