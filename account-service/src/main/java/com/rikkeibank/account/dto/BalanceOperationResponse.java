package com.rikkeibank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceOperationResponse {
    private String accountNumber;
    private String operationType; // DEBIT, CREDIT, DEPOSIT, WITHDRAW
    private BigDecimal amount;
    private BigDecimal previousBalance;
    private BigDecimal currentBalance;
    private String status; // SUCCESS, FAILED
    private String message;
    private LocalDateTime timestamp;
}
