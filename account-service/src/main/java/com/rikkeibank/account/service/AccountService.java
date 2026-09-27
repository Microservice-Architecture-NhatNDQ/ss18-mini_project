package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.AccountDto;
import com.rikkeibank.account.dto.BalanceOperationRequest;
import com.rikkeibank.account.dto.BalanceOperationResponse;
import com.rikkeibank.account.entity.BankAccount;
import com.rikkeibank.account.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final BankAccountRepository accountRepository;

    @Value("${rikkeibank.simulate-failure:false}")
    private boolean simulateGlobalFailure;

    @Transactional
    public AccountDto createAccount(AccountDto dto) {
        log.info("Creating bank account for customer ID: {}, type: {}", dto.getCustomerId(), dto.getAccountTypeCode());

        String accNumber = dto.getAccountNumber();
        if (accNumber == null || accNumber.isBlank()) {
            accNumber = "1000" + (100000 + new Random().nextInt(900000));
        }

        if (accountRepository.existsByAccountNumber(accNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account number already exists: " + accNumber);
        }

        BankAccount account = BankAccount.builder()
                .accountNumber(accNumber)
                .customerId(dto.getCustomerId())
                .accountTypeCode(dto.getAccountTypeCode().toUpperCase())
                .balance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO)
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "VND")
                .status("ACTIVE")
                .build();

        BankAccount saved = accountRepository.save(account);
        log.info("Bank account created successfully: Account No {}, Customer ID {}", saved.getAccountNumber(), saved.getCustomerId());
        return mapToDto(saved);
    }

    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "accounts", key = "#id")
    public AccountDto getAccountById(Long id) {
        log.info("[Cache-Aside] Cache MISS! Querying DB for Account ID: {}", id);
        BankAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with ID: " + id));
        return mapToDto(account);
    }

    @Cacheable(value = "accounts-by-number", key = "#accountNumber")
    public AccountDto getAccountByNumber(String accountNumber) {
        log.info("[Cache-Aside] Cache MISS! Querying DB for Account Number: {}", accountNumber);
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with number: " + accountNumber));
        return mapToDto(account);
    }

    public List<AccountDto> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accounts-by-number", key = "#accountNumber"),
            @CacheEvict(value = "accounts", allEntries = true)
    })
    public AccountDto updateStatus(String accountNumber, String status) {
        log.info("Updating status for account {}: {}", accountNumber, status);
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountNumber));

        account.setStatus(status.toUpperCase());
        BankAccount updated = accountRepository.save(account);
        return mapToDto(updated);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accounts-by-number", key = "#accountNumber"),
            @CacheEvict(value = "accounts", allEntries = true)
    })
    public BalanceOperationResponse debit(String accountNumber, BalanceOperationRequest req) {
        log.info("Processing DEBIT on account {}: amount = {}", accountNumber, req.getAmount());

        if (simulateGlobalFailure || req.isSimulateFailure()) {
            log.error("SIMULATED FAILURE triggered on debit for account {}", accountNumber);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated Failure: Debit operation failed unexpectedly");
        }

        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found: " + accountNumber));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source account is " + account.getStatus() + ", cannot perform debit");
        }

        if (account.getBalance().compareTo(req.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds in account " + accountNumber + ". Available balance: " + account.getBalance());
        }

        BigDecimal prevBalance = account.getBalance();
        BigDecimal newBalance = prevBalance.subtract(req.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("DEBIT successful for {}. Previous: {}, New: {}", accountNumber, prevBalance, newBalance);

        return BalanceOperationResponse.builder()
                .accountNumber(accountNumber)
                .operationType("DEBIT")
                .amount(req.getAmount())
                .previousBalance(prevBalance)
                .currentBalance(newBalance)
                .status("SUCCESS")
                .message("Debited successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accounts-by-number", key = "#accountNumber"),
            @CacheEvict(value = "accounts", allEntries = true)
    })
    public BalanceOperationResponse credit(String accountNumber, BalanceOperationRequest req) {
        log.info("Processing CREDIT on account {}: amount = {}", accountNumber, req.getAmount());

        if (simulateGlobalFailure || req.isSimulateFailure()) {
            log.error("SIMULATED FAILURE triggered on credit for account {}", accountNumber);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated Failure: Credit operation failed unexpectedly (triggers Saga Compensating Rollback)");
        }

        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found: " + accountNumber));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination account is " + account.getStatus() + ", cannot receive money");
        }

        BigDecimal prevBalance = account.getBalance();
        BigDecimal newBalance = prevBalance.add(req.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("CREDIT successful for {}. Previous: {}, New: {}", accountNumber, prevBalance, newBalance);

        return BalanceOperationResponse.builder()
                .accountNumber(accountNumber)
                .operationType("CREDIT")
                .amount(req.getAmount())
                .previousBalance(prevBalance)
                .currentBalance(newBalance)
                .status("SUCCESS")
                .message("Credited successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public AccountDto mapToDto(BankAccount entity) {
        return AccountDto.builder()
                .id(entity.getId())
                .accountNumber(entity.getAccountNumber())
                .customerId(entity.getCustomerId())
                .accountTypeCode(entity.getAccountTypeCode())
                .balance(entity.getBalance())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
