package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCode(String customerCode);
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByIdentityNumber(String identityNumber);
    boolean existsByIdentityNumber(String identityNumber);
    boolean existsByCustomerCode(String customerCode);
}
