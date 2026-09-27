package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.AccountTypeDto;
import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "account-types-all", allEntries = true)
    })
    public AccountTypeDto createAccountType(AccountTypeDto dto) {
        log.info("Creating account type catalog: {}", dto.getTypeCode());

        if (accountTypeRepository.existsByTypeCode(dto.getTypeCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account type code already exists");
        }

        AccountType entity = AccountType.builder()
                .typeCode(dto.getTypeCode().toUpperCase())
                .typeName(dto.getTypeName())
                .description(dto.getDescription())
                .interestRate(dto.getInterestRate())
                .minBalance(dto.getMinBalance())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "VND")
                .status("ACTIVE")
                .build();

        AccountType saved = accountTypeRepository.save(entity);
        return mapToDto(saved);
    }

    @Cacheable(value = "account-types-all")
    public List<AccountTypeDto> getAllAccountTypes() {
        log.info("[Cache-Aside] Cache MISS! Querying DB for ALL Account Types...");
        return accountTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "account-types", key = "#id")
    public AccountTypeDto getAccountTypeById(Long id) {
        log.info("[Cache-Aside] Cache MISS! Querying DB for Account Type ID: {}", id);
        AccountType entity = accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account type not found with ID: " + id));
        return mapToDto(entity);
    }

    public AccountTypeDto getAccountTypeByCode(String code) {
        AccountType entity = accountTypeRepository.findByTypeCode(code.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account type not found with code: " + code));
        return mapToDto(entity);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "account-types", key = "#id"),
            @CacheEvict(value = "account-types-all", allEntries = true)
    })
    public AccountTypeDto updateAccountType(Long id, AccountTypeDto dto) {
        log.info("[Cache-Aside] Evicting cache & updating Account Type ID: {}", id);
        AccountType entity = accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account type not found with ID: " + id));

        entity.setTypeName(dto.getTypeName());
        entity.setDescription(dto.getDescription());
        if (dto.getInterestRate() != null) {
            entity.setInterestRate(dto.getInterestRate());
        }
        if (dto.getMinBalance() != null) {
            entity.setMinBalance(dto.getMinBalance());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        AccountType updated = accountTypeRepository.save(entity);
        return mapToDto(updated);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "account-types", key = "#id"),
            @CacheEvict(value = "account-types-all", allEntries = true)
    })
    public void deleteAccountType(Long id) {
        AccountType entity = accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account type not found with ID: " + id));
        entity.setStatus("INACTIVE");
        accountTypeRepository.save(entity);
    }

    public AccountTypeDto mapToDto(AccountType entity) {
        return AccountTypeDto.builder()
                .id(entity.getId())
                .typeCode(entity.getTypeCode())
                .typeName(entity.getTypeName())
                .description(entity.getDescription())
                .interestRate(entity.getInterestRate())
                .minBalance(entity.getMinBalance())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
