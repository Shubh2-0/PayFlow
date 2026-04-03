package com.payflow.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.wallet.dto.CreateWalletRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.enums.WalletStatus;
import com.payflow.wallet.exception.GlobalExceptionHandler;
import com.payflow.wallet.exception.WalletNotFoundException;
import com.payflow.wallet.service.WalletService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import(GlobalExceptionHandler.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    private WalletResponse sampleWalletResponse() {
        return WalletResponse.builder()
                .id(1L)
                .userId("user-123")
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/wallets should create wallet and return 201")
    void test_createWallet_returns201() throws Exception {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId("user-123")
                .currency("INR")
                .build();

        when(walletService.createWallet(any())).thenReturn(sampleWalletResponse());

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/wallets/{id} should return wallet and 200")
    void test_getWallet_returns200() throws Exception {
        when(walletService.getWallet(1L)).thenReturn(sampleWalletResponse());

        mockMvc.perform(get("/api/wallets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @DisplayName("GET /api/wallets/{id} should return 404 when wallet not found")
    void test_getWallet_notFound_returns404() throws Exception {
        when(walletService.getWallet(999L)).thenThrow(new WalletNotFoundException("Wallet not found with id: 999"));

        mockMvc.perform(get("/api/wallets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found with id: 999"));
    }

    @Test
    @DisplayName("PUT /api/wallets/{id}/freeze should freeze wallet and return 200")
    void test_freezeWallet_returns200() throws Exception {
        WalletResponse frozenResponse = sampleWalletResponse();
        frozenResponse.setStatus(WalletStatus.FROZEN);

        when(walletService.freezeWallet(1L)).thenReturn(frozenResponse);

        mockMvc.perform(put("/api/wallets/1/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }
}
