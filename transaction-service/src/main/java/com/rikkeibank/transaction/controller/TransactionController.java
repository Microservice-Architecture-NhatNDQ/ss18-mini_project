package com.rikkeibank.transaction.controller;

import com.rikkeibank.transaction.dto.*;
import com.rikkeibank.transaction.service.TransactionSagaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionSagaService sagaService;

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transferMoney(
            @Valid @RequestBody TransferRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        log.info("REST request to transfer money from: {} to: {}", req.getFromAccountNumber(), req.getToAccountNumber());
        Long customerId = (userIdHeader != null && !userIdHeader.isBlank()) ? Long.parseLong(userIdHeader) : null;

        TransferResponse response = sagaService.executeTransferWithCircuitBreaker(req, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(@RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        List<TransactionDto> list = sagaService.getAllTransactions();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/transactions/{referenceCode}")
    public ResponseEntity<TransactionDto> getTransactionByRef(@PathVariable String referenceCode) {
        TransactionDto tx = sagaService.getTransactionByRef(referenceCode);
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/transactions/account/{accountNumber}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccount(@PathVariable String accountNumber) {
        List<TransactionDto> list = sagaService.getTransactionsByAccount(accountNumber);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/transactions/daily")
    public ResponseEntity<?> getDailyReport(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "tellerId", required = false) Long tellerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN or TELLER can view daily reports"));
        }

        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        // If user is a TELLER (and not ADMIN), limit to their own teller ID (scope isolation)
        if (roles != null && roles.contains("ROLE_TELLER") && !roles.contains("ROLE_ADMIN")) {
            if (userId != null && !userId.isBlank()) {
                tellerId = Long.parseLong(userId);
            }
        }

        DailyReportDto report = sagaService.getDailyReport(reportDate, tellerId);
        return ResponseEntity.ok(report);
    }
}
