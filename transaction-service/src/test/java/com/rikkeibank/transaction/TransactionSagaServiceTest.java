package com.rikkeibank.transaction;

import com.rikkeibank.transaction.client.AccountClient;
import com.rikkeibank.transaction.dto.BalanceOperationRequest;
import com.rikkeibank.transaction.dto.BalanceOperationResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.dto.TransferResponse;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.repository.TransactionRepository;
import com.rikkeibank.transaction.service.TransactionSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSagaServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionSagaService sagaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sagaService, "maxTransferLimit", new BigDecimal("1000000000"));
        ReflectionTestUtils.setField(sagaService, "simulateFailureGlobal", false);
    }

    @Test
    @DisplayName("Saga Happy Path: Money Transfer completes successfully through Debit and Credit steps")
    void testExecuteTransfer_Success() {
        TransferRequest req = TransferRequest.builder()
                .fromAccountNumber("1000123456")
                .toAccountNumber("1000654321")
                .amount(new BigDecimal("500000.00"))
                .description("Chuyen tien mua hang")
                .build();

        Transaction mockTx = Transaction.builder()
                .id(1L)
                .referenceCode("TXN-123456")
                .fromAccountNumber("1000123456")
                .toAccountNumber("1000654321")
                .amount(new BigDecimal("500000.00"))
                .status("PENDING")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTx);

        BalanceOperationResponse debitRes = BalanceOperationResponse.builder()
                .accountNumber("1000123456")
                .status("SUCCESS")
                .amount(new BigDecimal("500000.00"))
                .build();

        BalanceOperationResponse creditRes = BalanceOperationResponse.builder()
                .accountNumber("1000654321")
                .status("SUCCESS")
                .amount(new BigDecimal("500000.00"))
                .build();

        when(accountClient.debit(eq("1000123456"), any(BalanceOperationRequest.class))).thenReturn(debitRes);
        when(accountClient.credit(eq("1000654321"), any(BalanceOperationRequest.class))).thenReturn(creditRes);

        TransferResponse res = sagaService.executeTransfer(req, 100L);

        assertNotNull(res);
        assertEquals("SUCCESS", res.getStatus());
        assertEquals(new BigDecimal("500000.00"), res.getAmount());

        // Verify that Kafka events were sent for SAGA_TRANSFER_STARTED, SAGA_DEBITED, SAGA_TRANSFER_COMPLETED
        verify(kafkaTemplate, atLeast(3)).send(eq(TransactionSagaService.TOPIC_TRANSACTIONS), anyString(), any());
    }

    @Test
    @DisplayName("Saga Compensating Rollback: Destination credit fails -> Triggers Compensating Refund to source account")
    void testExecuteTransfer_CreditFails_TriggersCompensatingRollback() {
        TransferRequest req = TransferRequest.builder()
                .fromAccountNumber("1000123456")
                .toAccountNumber("1000654321")
                .amount(new BigDecimal("500000.00"))
                .description("Chuyen tien test rollback")
                .build();

        Transaction mockTx = Transaction.builder()
                .id(1L)
                .referenceCode("TXN-123456")
                .fromAccountNumber("1000123456")
                .toAccountNumber("1000654321")
                .amount(new BigDecimal("500000.00"))
                .status("PENDING")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTx);

        // Step 1: Debit succeeds
        BalanceOperationResponse debitRes = BalanceOperationResponse.builder()
                .accountNumber("1000123456")
                .status("SUCCESS")
                .amount(new BigDecimal("500000.00"))
                .build();
        when(accountClient.debit(eq("1000123456"), any(BalanceOperationRequest.class))).thenReturn(debitRes);

        // Step 2: Credit FAILS (simulating locked account or network error)
        when(accountClient.credit(eq("1000654321"), any(BalanceOperationRequest.class)))
                .thenThrow(new RuntimeException("Destination account is locked"));

        // Expect Exception to be thrown
        assertThrows(ResponseStatusException.class, () -> sagaService.executeTransfer(req, 100L));

        // Verify COMPENSATING ACTION: Source account was credited back the deducted amount (Refund)
        verify(accountClient, times(1)).credit(eq("1000123456"), argThat(r ->
                r.getAmount().compareTo(new BigDecimal("500000.00")) == 0 &&
                        r.getDescription().contains("Hoan tien")
        ));

        // Verify Kafka Compensating event was published
        verify(kafkaTemplate, atLeast(1)).send(eq(TransactionSagaService.TOPIC_TRANSACTIONS), anyString(), any());
    }
}
