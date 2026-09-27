package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.client.AccountClient;
import com.rikkeibank.transaction.dto.*;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.event.TransactionEvent;
import com.rikkeibank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSagaService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_TRANSACTIONS = "transaction-events";

    @Value("${rikkeibank.max-transfer-limit:1000000000}")
    private BigDecimal maxTransferLimit;

    @Value("${rikkeibank.simulate-failure:false}")
    private boolean simulateFailureGlobal;

    /**
     * SAGA ORCHESTRATOR: Executes multi-step distributed money transfer with Compensating Rollback
     */
    public TransferResponse executeTransfer(TransferRequest req, Long customerId) {
        log.info("Starting Saga Money Transfer from {} to {}, amount = {}",
                req.getFromAccountNumber(), req.getToAccountNumber(), req.getAmount());

        // 1. Validations
        if (req.getFromAccountNumber().equals(req.getToAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must be different");
        }

        if (req.getAmount().compareTo(maxTransferLimit) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount exceeds maximum allowed limit of " + maxTransferLimit + " VND");
        }

        String referenceCode = "TXN-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();

        // 2. Initialize Transaction in DB
        Transaction transaction = Transaction.builder()
                .referenceCode(referenceCode)
                .fromAccountNumber(req.getFromAccountNumber())
                .toAccountNumber(req.getToAccountNumber())
                .amount(req.getAmount())
                .fee(BigDecimal.ZERO)
                .currency("VND")
                .type("TRANSFER")
                .status("PENDING")
                .description(req.getDescription() != null ? req.getDescription() : "Chuyen tien qua RikkeiBank API")
                .customerId(customerId)
                .tellerId(req.getTellerId())
                .build();

        Transaction savedTx = transactionRepository.save(transaction);

        // Publish SAGA_TRANSFER_STARTED Event
        publishEvent("SAGA_TRANSFER_STARTED", savedTx, "Saga transfer initiated");

        // Step 1: Execute Debit on Source Account
        boolean isDebited = false;
        try {
            savedTx.setStatus("PROCESSING");
            transactionRepository.save(savedTx);

            log.info("[Saga Step 1] Calling Account Service to DEBIT {} from account {}", req.getAmount(), req.getFromAccountNumber());
            BalanceOperationRequest debitReq = BalanceOperationRequest.builder()
                    .amount(req.getAmount())
                    .transactionRef(referenceCode)
                    .description("Chuyen tien den " + req.getToAccountNumber())
                    .tellerId(req.getTellerId())
                    .simulateFailure(simulateFailureGlobal)
                    .build();

            BalanceOperationResponse debitRes = accountClient.debit(req.getFromAccountNumber(), debitReq);
            if (!"SUCCESS".equalsIgnoreCase(debitRes.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit failed: " + debitRes.getMessage());
            }
            isDebited = true;
            publishEvent("SAGA_DEBITED", savedTx, "Source account debited successfully");

        } catch (Exception e) {
            log.error("[Saga Step 1 Failed] Debit error: {}", e.getMessage());
            savedTx.setStatus("FAILED");
            savedTx.setFailureReason("Debit step failed: " + e.getMessage());
            transactionRepository.save(savedTx);
            publishEvent("SAGA_TRANSFER_FAILED", savedTx, savedTx.getFailureReason());

            if (e instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transfer failed during debit: " + e.getMessage());
        }

        // Step 2: Execute Credit on Destination Account (with Compensating Rollback if failed)
        try {
            log.info("[Saga Step 2] Calling Account Service to CREDIT {} to account {}", req.getAmount(), req.getToAccountNumber());
            BalanceOperationRequest creditReq = BalanceOperationRequest.builder()
                    .amount(req.getAmount())
                    .transactionRef(referenceCode)
                    .description("Nhan tien tu " + req.getFromAccountNumber() + ": " + req.getDescription())
                    .tellerId(req.getTellerId())
                    .simulateFailure(req.isSimulateCreditFailure() || simulateFailureGlobal)
                    .build();

            BalanceOperationResponse creditRes = accountClient.credit(req.getToAccountNumber(), creditReq);
            if (!"SUCCESS".equalsIgnoreCase(creditRes.getStatus())) {
                throw new RuntimeException("Credit response status: " + creditRes.getStatus());
            }

            // Both steps succeeded! SAGA Complete
            savedTx.setStatus("SUCCESS");
            Transaction completedTx = transactionRepository.save(savedTx);
            log.info("[Saga SUCCESS] Transfer {} completed successfully!", referenceCode);

            publishEvent("SAGA_TRANSFER_COMPLETED", completedTx, "Transfer completed successfully");

            return TransferResponse.builder()
                    .referenceCode(referenceCode)
                    .fromAccountNumber(completedTx.getFromAccountNumber())
                    .toAccountNumber(completedTx.getToAccountNumber())
                    .amount(completedTx.getAmount())
                    .fee(completedTx.getFee())
                    .currency(completedTx.getCurrency())
                    .status("SUCCESS")
                    .description(completedTx.getDescription())
                    .message("Giao dịch chuyển tiền thành công!")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.warn("[Saga Step 2 Failed] Credit step failed ({}). Initiating COMPENSATING ROLLBACK...", e.getMessage());

            // COMPENSATING ACTION: Refund debited amount back to source account
            if (isDebited) {
                try {
                    log.info("[Saga Compensating] Refunding {} back to source account {}", req.getAmount(), req.getFromAccountNumber());
                    BalanceOperationRequest refundReq = BalanceOperationRequest.builder()
                            .amount(req.getAmount())
                            .transactionRef(referenceCode + "-REFUND")
                            .description("Hoan tien giao dich chuyen khoan loi: " + referenceCode)
                            .build();
                    accountClient.credit(req.getFromAccountNumber(), refundReq);
                    log.info("[Saga Compensating] Refund to {} completed successfully!", req.getFromAccountNumber());
                } catch (Exception ex) {
                    log.error("[Saga Critical] Compensating refund failed: {}", ex.getMessage());
                }
            }

            savedTx.setStatus("ROLLED_BACK");
            savedTx.setFailureReason("Credit to destination account failed: " + e.getMessage() + ". Deducted funds refunded back to source.");
            Transaction rolledBackTx = transactionRepository.save(savedTx);

            publishEvent("SAGA_TRANSFER_COMPENSATED", rolledBackTx, rolledBackTx.getFailureReason());

            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Giao dịch chuyển tiền thất bại do tài khoản nhận không hợp lệ hoặc lỗi hệ thống. Số dư đã được hoàn lại (Saga Compensating Rollback). Chi tiết: " + e.getMessage());
        }
    }

    /**
     * Resilient transfer with Circuit Breaker protection
     */
    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "transferCircuitBreakerFallback")
    public TransferResponse executeTransferWithCircuitBreaker(TransferRequest req, Long customerId) {
        return executeTransfer(req, customerId);
    }

    public TransferResponse transferCircuitBreakerFallback(TransferRequest req, Long customerId, Throwable t) {
        log.error("Circuit Breaker OPEN or Account Service Unavailable! Fallback triggered. Error: {}", t.getMessage());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Hệ thống quản lý tài khoản đang bận hoặc gián đoạn kết nối. Vui lòng thử lại sau ít phút (Resilience4j Circuit Breaker Fallback)");
    }

    private void publishEvent(String eventType, Transaction tx, String message) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .eventType(eventType)
                    .referenceCode(tx.getReferenceCode())
                    .fromAccountNumber(tx.getFromAccountNumber())
                    .toAccountNumber(tx.getToAccountNumber())
                    .amount(tx.getAmount())
                    .status(tx.getStatus())
                    .message(message)
                    .customerId(tx.getCustomerId())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC_TRANSACTIONS, tx.getReferenceCode(), event);
            log.info("Kafka event published to topic {}: {}", TOPIC_TRANSACTIONS, eventType);
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event (Kafka might be offline): {}", e.getMessage());
        }
    }

    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public TransactionDto getTransactionByRef(String referenceCode) {
        Transaction tx = transactionRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: " + referenceCode));
        return mapToDto(tx);
    }

    public List<TransactionDto> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DailyReportDto getDailyReport(LocalDate date, Long tellerId) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Transaction> txList;
        if (tellerId != null) {
            txList = transactionRepository.findDailyTransactionsByTeller(tellerId, start, end);
        } else {
            txList = transactionRepository.findDailyTransactions(start, end);
        }

        long successCount = txList.stream().filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus())).count();
        long failedCount = txList.size() - successCount;
        BigDecimal totalAmount = txList.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailyReportDto.builder()
                .reportDate(date)
                .tellerId(tellerId)
                .totalTransactions(txList.size())
                .successfulTransactions(successCount)
                .failedTransactions(failedCount)
                .totalAmountTransferred(totalAmount)
                .transactions(txList.stream().map(this::mapToDto).collect(Collectors.toList()))
                .build();
    }

    public TransactionDto mapToDto(Transaction tx) {
        return TransactionDto.builder()
                .id(tx.getId())
                .referenceCode(tx.getReferenceCode())
                .fromAccountNumber(tx.getFromAccountNumber())
                .toAccountNumber(tx.getToAccountNumber())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .currency(tx.getCurrency())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .customerId(tx.getCustomerId())
                .tellerId(tx.getTellerId())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
