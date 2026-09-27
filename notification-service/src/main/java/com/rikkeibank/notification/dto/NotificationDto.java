package com.rikkeibank.notification.dto;

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
public class NotificationDto {
    private Long id;
    private Long customerId;
    private String title;
    private String message;
    private String eventType;
    private String referenceCode;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
