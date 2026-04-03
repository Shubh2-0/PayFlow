package com.payflow.wallet.service;

import com.payflow.wallet.dto.CreateWalletRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.enums.WalletStatus;
import com.payflow.wallet.exception.DuplicateWalletException;
import com.payflow.wallet.exception.InvalidTransactionException;
import com.payflow.wallet.exception.WalletNotFoundException;
import com.payflow.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    @DisplayName("Should create wallet successfully for new user")
    void test_createWallet_success() {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId("user-123")
                .currency("INR")
                .build();

        Wallet savedWallet = Wallet.builder()
                .id(1L)
                .userId("user-123")
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(walletRepository.existsByUserId("user-123")).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        WalletResponse response = walletService.createWallet(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw DuplicateWalletException when wallet already exists")
    void test_createWallet_alreadyExists_throwsException() {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId("user-123")
                .build();

        when(walletRepository.existsByUserId("user-123")).thenReturn(true);

        assertThatThrownBy(() -> walletService.createWallet(request))
                .isInstanceOf(DuplicateWalletException.class)
                .hasMessageContaining("user-123");

        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return wallet when found by ID")
    void test_getWallet_success() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user-123")
                .balance(new BigDecimal("5000.0000"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWallet(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet not found")
    void test_getWallet_notFound_throwsException() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(999L))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Should freeze an active wallet successfully")
    void test_freezeWallet_success() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user-123")
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.freezeWallet(1L);

        assertThat(response.getStatus()).isEqualTo(WalletStatus.FROZEN);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to freeze a closed wallet")
    void test_freezeWallet_alreadyClosed_throwsException() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user-123")
                .status(WalletStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.freezeWallet(1L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("closed");
    }
}
