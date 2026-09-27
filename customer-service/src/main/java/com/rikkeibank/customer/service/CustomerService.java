package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.CustomerDto;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto) {
        log.info("Creating customer with CCCD: {}", dto.getIdentityNumber());

        if (customerRepository.existsByIdentityNumber(dto.getIdentityNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Identity number (CCCD) already exists in the system");
        }

        String code = dto.getCustomerCode();
        if (code == null || code.isBlank()) {
            code = "CUST" + (System.currentTimeMillis() % 100000);
        }

        Customer customer = Customer.builder()
                .customerCode(code)
                .userId(dto.getUserId())
                .fullName(dto.getFullName())
                .identityNumber(dto.getIdentityNumber())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .dateOfBirth(dto.getDateOfBirth())
                .address(dto.getAddress())
                .status("ACTIVE")
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully with ID: {}, Code: {}", saved.getId(), saved.getCustomerCode());
        return mapToDto(saved);
    }

    public List<CustomerDto> getAllCustomers() {
        log.info("Fetching all customers from database");
        return customerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "customers", key = "#id")
    public CustomerDto getCustomerById(Long id) {
        log.info("[Cache-Aside] Cache MISS! Querying DB for Customer ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));
        return mapToDto(customer);
    }

    public CustomerDto getCustomerByUserId(Long userId) {
        log.info("Fetching customer by User ID: {}", userId);
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found for user ID: " + userId));
        return mapToDto(customer);
    }

    public CustomerDto getCustomerByCode(String code) {
        log.info("Fetching customer by Code: {}", code);
        Customer customer = customerRepository.findByCustomerCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with code: " + code));
        return mapToDto(customer);
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public CustomerDto updateCustomer(Long id, CustomerDto dto) {
        log.info("[Cache-Aside] Evicting cache & updating Customer ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        customer.setFullName(dto.getFullName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        if (dto.getDateOfBirth() != null) {
            customer.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getStatus() != null) {
            customer.setStatus(dto.getStatus());
        }

        Customer updated = customerRepository.save(customer);
        return mapToDto(updated);
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public void deleteCustomer(Long id) {
        log.info("[Cache-Aside] Evicting cache & deleting Customer ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));
        customer.setStatus("INACTIVE");
        customerRepository.save(customer);
    }

    public CustomerDto mapToDto(Customer entity) {
        return CustomerDto.builder()
                .id(entity.getId())
                .customerCode(entity.getCustomerCode())
                .userId(entity.getUserId())
                .fullName(entity.getFullName())
                .identityNumber(entity.getIdentityNumber())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .dateOfBirth(entity.getDateOfBirth())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
