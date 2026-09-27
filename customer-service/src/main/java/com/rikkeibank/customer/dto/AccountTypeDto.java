package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeDto implements Serializable {
    private Long id;

    @NotBlank(message = "Type code is required")
    private String typeCode;

    @NotBlank(message = "Type name is required")
    private String typeName;

    private String description;
    private BigDecimal interestRate;
    private BigDecimal minBalance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
