package com.rikkeibank.transaction.event;

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
public class TransactionEvent {
    private String eventType; // SAGA_TRANSFER_STARTED, SAGA_DEBITED, SAGA_CREDITED, SAGA_TRANSFER_COMPLETED, SAGA_COMPENSATING, SAGA_TRANSFER_FAILED
    private String referenceCode;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED, ROLLED_BACK
    private String message;
    private Long customerId;
    private LocalDateTime timestamp;
}
