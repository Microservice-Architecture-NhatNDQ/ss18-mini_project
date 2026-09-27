package com.rikkeibank.customer;

import com.rikkeibank.customer.dto.CustomerDto;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(1L)
                .customerCode("CUST001")
                .userId(100L)
                .fullName("Nguyen Van A")
                .identityNumber("001234567890")
                .phone("0912345678")
                .email("nva@gmail.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("Ha Noi")
                .status("ACTIVE")
                .build();
    }

    @Test
    void testGetCustomerById_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        CustomerDto result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals("CUST001", result.getCustomerCode());
        assertEquals("Nguyen Van A", result.getFullName());
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void testCreateCustomer_DuplicateIdentityNumber_ThrowsConflict() {
        CustomerDto dto = CustomerDto.builder()
                .identityNumber("001234567890")
                .fullName("Nguyen Van A")
                .build();

        when(customerRepository.existsByIdentityNumber("001234567890")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> customerService.createCustomer(dto));
    }
}
