package com.rikkeibank.transaction.client;

import com.rikkeibank.transaction.dto.AccountDto;
import com.rikkeibank.transaction.dto.BalanceOperationRequest;
import com.rikkeibank.transaction.dto.BalanceOperationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/number/{accountNumber}")
    AccountDto getAccountByNumber(@PathVariable("accountNumber") String accountNumber);

    @PutMapping("/api/v1/accounts/{accountNumber}/debit")
    BalanceOperationResponse debit(@PathVariable("accountNumber") String accountNumber, @RequestBody BalanceOperationRequest req);

    @PutMapping("/api/v1/accounts/{accountNumber}/credit")
    BalanceOperationResponse credit(@PathVariable("accountNumber") String accountNumber, @RequestBody BalanceOperationRequest req);
}
