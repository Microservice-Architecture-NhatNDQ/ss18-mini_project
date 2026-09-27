package com.rikkeibank.transaction.dto;

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
public class BalanceOperationRequest {
    private BigDecimal amount;
    private String transactionRef;
    private String description;
    private Long tellerId;
    private boolean simulateFailure;
}
