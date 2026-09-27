package com.rikkeibank.account.config;

import com.rikkeibank.account.entity.BankAccount;
import com.rikkeibank.account.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BankAccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            log.info("Seeding Bank Accounts for RikkeiBank...");

            // Account 1 for Customer 1 (Tran Van Khach Hang A - CUST001 / ID 1)
            accountRepository.save(BankAccount.builder()
                    .accountNumber("1000123456")
                    .customerId(1L)
                    .accountTypeCode("CHECKING")
                    .balance(new BigDecimal("100000000.00")) // 100 Million VND
                    .currency("VND")
                    .status("ACTIVE")
                    .build());

            // Account 2 for Customer 2 (Le Thi Khach Hang B - CUST002 / ID 2)
            accountRepository.save(BankAccount.builder()
                    .accountNumber("1000654321")
                    .customerId(2L)
                    .accountTypeCode("CHECKING")
                    .balance(new BigDecimal("50000000.00")) // 50 Million VND
                    .currency("VND")
                    .status("ACTIVE")
                    .build());

            // Account 3 for Customer 1 (Savings)
            accountRepository.save(BankAccount.builder()
                    .accountNumber("1000999888")
                    .customerId(1L)
                    .accountTypeCode("SAVINGS_REGULAR")
                    .balance(new BigDecimal("200000000.00")) // 200 Million VND
                    .currency("VND")
                    .status("ACTIVE")
                    .build());

            log.info("Bank Accounts seeded successfully.");
        }
    }
}
