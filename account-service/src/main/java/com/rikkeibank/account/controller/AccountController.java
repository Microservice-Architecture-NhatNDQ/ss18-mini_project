package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.*;
import com.rikkeibank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<?> createAccount(
            @Valid @RequestBody AccountDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        AccountDto created = accountService.createAccount(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<?> getAllAccounts(@RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        List<AccountDto> list = accountService.getAllAccounts();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        AccountDto account = accountService.getAccountById(id);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountDto> getAccountByNumber(@PathVariable String accountNumber) {
        AccountDto account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountDto>> getAccountsByCustomerId(@PathVariable Long customerId) {
        List<AccountDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateStatusRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        AccountDto updated = accountService.updateStatus(accountNumber, req.getStatus());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{accountNumber}/debit")
    public ResponseEntity<BalanceOperationResponse> debit(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceOperationRequest req) {

        BalanceOperationResponse response = accountService.debit(accountNumber, req);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<BalanceOperationResponse> credit(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceOperationRequest req) {

        BalanceOperationResponse response = accountService.credit(accountNumber, req);
        return ResponseEntity.ok(response);
    }
}
