package com.rikkeibank.customer.config;

import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.AccountTypeRepository;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final AccountTypeRepository accountTypeRepository;

    @Override
    public void run(String... args) {
        // 1. Preload Account Types
        if (accountTypeRepository.count() == 0) {
            log.info("Seeding Account Type Catalog...");
            accountTypeRepository.save(AccountType.builder()
                    .typeCode("CHECKING")
                    .typeName("Tài khoản Thanh toán RikkeiPay")
                    .description("Tài khoản thanh toán không kỳ hạn, miễn phí chuyển khoản 24/7")
                    .interestRate(new BigDecimal("0.20"))
                    .minBalance(new BigDecimal("50000"))
                    .currency("VND")
                    .status("ACTIVE")
                    .build());

            accountTypeRepository.save(AccountType.builder()
                    .typeCode("SAVINGS_REGULAR")
                    .typeName("Tiết kiệm Thường RikkeiSave")
                    .description("Tiết kiệm tích lũy linh hoạt theo kỳ hạn")
                    .interestRate(new BigDecimal("5.50"))
                    .minBalance(new BigDecimal("1000000"))
                    .currency("VND")
                    .status("ACTIVE")
                    .build());

            accountTypeRepository.save(AccountType.builder()
                    .typeCode("VIP_GOLD")
                    .typeName("Tài khoản Kim Cương Rikkei VIP")
                    .description("Đặc quyền ưu tiên giao dịch và hạn mức chuyển tiền không giới hạn")
                    .interestRate(new BigDecimal("1.00"))
                    .minBalance(new BigDecimal("50000000"))
                    .currency("VND")
                    .status("ACTIVE")
                    .build());
        }

        // 2. Preload Staff
        if (staffRepository.count() == 0) {
            log.info("Seeding Staff members...");
            staffRepository.save(Staff.builder()
                    .staffCode("STF001")
                    .userId(2L)
                    .fullName("Nguyen Van Giao Dich (Teller 01)")
                    .email("teller01@rikkeibank.com")
                    .phone("0912345678")
                    .branchCode("HN-HOANG-MAI")
                    .department("Front Office Teller")
                    .role("TELLER")
                    .status("ACTIVE")
                    .build());
        }

        // 3. Preload Customers
        if (customerRepository.count() == 0) {
            log.info("Seeding Customers...");
            customerRepository.save(Customer.builder()
                    .customerCode("CUST001")
                    .userId(3L)
                    .fullName("Tran Van Khach Hang A")
                    .identityNumber("001095012345")
                    .phone("0987654321")
                    .email("customer01@gmail.com")
                    .dateOfBirth(LocalDate.of(1995, 5, 20))
                    .address("123 Pho Hue, Hai Ba Trung, Ha Noi")
                    .status("ACTIVE")
                    .build());

            customerRepository.save(Customer.builder()
                    .customerCode("CUST002")
                    .userId(4L)
                    .fullName("Le Thi Khach Hang B")
                    .identityNumber("001098054321")
                    .phone("0988776655")
                    .email("customer02@gmail.com")
                    .dateOfBirth(LocalDate.of(1998, 8, 15))
                    .address("456 Nguyen Trai, Thanh Xuan, Ha Noi")
                    .status("ACTIVE")
                    .build());
        }
    }
}
