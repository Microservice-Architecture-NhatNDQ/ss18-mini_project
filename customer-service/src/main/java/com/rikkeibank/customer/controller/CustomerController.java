package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.CustomerDto;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<?> createCustomer(
            @Valid @RequestBody CustomerDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        // ADMIN or TELLER can create customer profile
        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        CustomerDto created = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<?> getAllCustomers(@RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        List<CustomerDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        CustomerDto customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<CustomerDto> getCustomerByCode(@PathVariable String code) {
        CustomerDto customer = customerService.getCustomerByCode(code);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User header missing"));
        }
        CustomerDto customer = customerService.getCustomerByUserId(Long.parseLong(userId));
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
        }
        CustomerDto updated = customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can delete customers"));
        }
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(Map.of("message", "Customer deactivated successfully", "id", id));
    }
}
