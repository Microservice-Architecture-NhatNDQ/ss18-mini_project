package com.rikkeibank.account;

import com.rikkeibank.account.dto.BalanceOperationRequest;
import com.rikkeibank.account.dto.BalanceOperationResponse;
import com.rikkeibank.account.entity.BankAccount;
import com.rikkeibank.account.repository.BankAccountRepository;
import com.rikkeibank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private BankAccount sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = BankAccount.builder()
                .id(1L)
                .accountNumber("1000123456")
                .customerId(1L)
                .accountTypeCode("CHECKING")
                .balance(new BigDecimal("1000000.00"))
                .currency("VND")
                .status("ACTIVE")
                .build();
    }

    @Test
    void testDebit_Success() {
        BalanceOperationRequest req = BalanceOperationRequest.builder()
                .amount(new BigDecimal("200000.00"))
                .build();

        when(accountRepository.findByAccountNumber("1000123456")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(BankAccount.class))).thenReturn(sampleAccount);

        BalanceOperationResponse res = accountService.debit("1000123456", req);

        assertNotNull(res);
        assertEquals("DEBIT", res.getOperationType());
        assertEquals(new BigDecimal("800000.00"), res.getCurrentBalance());
        assertEquals("SUCCESS", res.getStatus());
    }

    @Test
    void testDebit_InsufficientFunds_ThrowsException() {
        BalanceOperationRequest req = BalanceOperationRequest.builder()
                .amount(new BigDecimal("2000000.00"))
                .build();

        when(accountRepository.findByAccountNumber("1000123456")).thenReturn(Optional.of(sampleAccount));

        assertThrows(ResponseStatusException.class, () -> accountService.debit("1000123456", req));
    }

    @Test
    void testCredit_Success() {
        BalanceOperationRequest req = BalanceOperationRequest.builder()
                .amount(new BigDecimal("500000.00"))
                .build();

        when(accountRepository.findByAccountNumber("1000123456")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(BankAccount.class))).thenReturn(sampleAccount);

        BalanceOperationResponse res = accountService.credit("1000123456", req);

        assertNotNull(res);
        assertEquals("CREDIT", res.getOperationType());
        assertEquals(new BigDecimal("1500000.00"), res.getCurrentBalance());
        assertEquals("SUCCESS", res.getStatus());
    }
}
