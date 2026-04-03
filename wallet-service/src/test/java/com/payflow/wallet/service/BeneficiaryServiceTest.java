package com.payflow.wallet.service;

import com.payflow.wallet.dto.BeneficiaryRequest;
import com.payflow.wallet.dto.BeneficiaryResponse;
import com.payflow.wallet.entity.BeneficiaryWallet;
import com.payflow.wallet.exception.DuplicateWalletException;
import com.payflow.wallet.exception.WalletNotFoundException;
import com.payflow.wallet.repository.BeneficiaryWalletRepository;
import com.payflow.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryWalletRepository beneficiaryWalletRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    @Test
    @DisplayName("Should add beneficiary successfully")
    void test_addBeneficiary_success() {
        BeneficiaryRequest request = BeneficiaryRequest.builder()
                .beneficiaryWalletId(2L)
                .nickname("John's Wallet")
                .build();

        BeneficiaryWallet saved = BeneficiaryWallet.builder()
                .id(1L)
                .ownerWalletId(1L)
                .beneficiaryWalletId(2L)
                .nickname("John's Wallet")
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.existsById(1L)).thenReturn(true);
        when(walletRepository.existsById(2L)).thenReturn(true);
        when(beneficiaryWalletRepository.existsByOwnerWalletIdAndBeneficiaryWalletId(1L, 2L)).thenReturn(false);
        when(beneficiaryWalletRepository.save(any(BeneficiaryWallet.class))).thenReturn(saved);

        BeneficiaryResponse response = beneficiaryService.addBeneficiary(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getBeneficiaryWalletId()).isEqualTo(2L);
        assertThat(response.getNickname()).isEqualTo("John's Wallet");
        verify(beneficiaryWalletRepository).save(any(BeneficiaryWallet.class));
    }

    @Test
    @DisplayName("Should throw DuplicateWalletException when beneficiary already exists")
    void test_addBeneficiary_duplicate_throwsException() {
        BeneficiaryRequest request = BeneficiaryRequest.builder()
                .beneficiaryWalletId(2L)
                .nickname("John's Wallet")
                .build();

        when(walletRepository.existsById(1L)).thenReturn(true);
        when(walletRepository.existsById(2L)).thenReturn(true);
        when(beneficiaryWalletRepository.existsByOwnerWalletIdAndBeneficiaryWalletId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> beneficiaryService.addBeneficiary(1L, request))
                .isInstanceOf(DuplicateWalletException.class)
                .hasMessageContaining("already exists");

        verify(beneficiaryWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should remove beneficiary successfully")
    void test_removeBeneficiary_success() {
        BeneficiaryWallet beneficiary = BeneficiaryWallet.builder()
                .id(1L)
                .ownerWalletId(1L)
                .beneficiaryWalletId(2L)
                .nickname("John's Wallet")
                .build();

        when(beneficiaryWalletRepository.findById(1L)).thenReturn(Optional.of(beneficiary));

        beneficiaryService.removeBeneficiary(1L, 1L);

        verify(beneficiaryWalletRepository).delete(beneficiary);
    }
}
