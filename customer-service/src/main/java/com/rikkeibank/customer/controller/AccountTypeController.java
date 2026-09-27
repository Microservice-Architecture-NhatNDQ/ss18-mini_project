package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.AccountTypeDto;
import com.rikkeibank.customer.service.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account-types")
@RequiredArgsConstructor
@Slf4j
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @PostMapping
    public ResponseEntity<?> createAccountType(
            @Valid @RequestBody AccountTypeDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can create account types"));
        }
        AccountTypeDto created = accountTypeService.createAccountType(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AccountTypeDto>> getAllAccountTypes() {
        List<AccountTypeDto> list = accountTypeService.getAllAccountTypes();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountTypeDto> getAccountTypeById(@PathVariable Long id) {
        AccountTypeDto type = accountTypeService.getAccountTypeById(id);
        return ResponseEntity.ok(type);
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<AccountTypeDto> getAccountTypeByCode(@PathVariable String code) {
        AccountTypeDto type = accountTypeService.getAccountTypeByCode(code);
        return ResponseEntity.ok(type);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccountType(
            @PathVariable Long id,
            @Valid @RequestBody AccountTypeDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can update account types"));
        }
        AccountTypeDto updated = accountTypeService.updateAccountType(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccountType(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can delete account types"));
        }
        accountTypeService.deleteAccountType(id);
        return ResponseEntity.ok(Map.of("message", "Account type deactivated successfully", "id", id));
    }
}
