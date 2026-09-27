package com.rikkeibank.identity.config;

import com.rikkeibank.identity.entity.Role;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial users for RikkeiBank...");

            // 1. Admin
            userRepository.save(User.builder()
                    .email("admin@rikkeibank.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("RikkeiBank System Administrator")
                    .phone("0901234567")
                    .role(Role.ROLE_ADMIN)
                    .status("ACTIVE")
                    .build());

            // 2. Teller
            userRepository.save(User.builder()
                    .email("teller01@rikkeibank.com")
                    .password(passwordEncoder.encode("teller123"))
                    .fullName("Nguyen Van Giao Dich (Teller 01)")
                    .phone("0912345678")
                    .role(Role.ROLE_TELLER)
                    .status("ACTIVE")
                    .build());

            // 3. Customer 1
            userRepository.save(User.builder()
                    .email("customer01@gmail.com")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Tran Van Khach Hang A")
                    .phone("0987654321")
                    .role(Role.ROLE_CUSTOMER)
                    .status("ACTIVE")
                    .build());

            // 4. Customer 2
            userRepository.save(User.builder()
                    .email("customer02@gmail.com")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Le Thi Khach Hang B")
                    .phone("0988776655")
                    .role(Role.ROLE_CUSTOMER)
                    .status("ACTIVE")
                    .build());

            log.info("Initial users seeded successfully: Admin, Teller, and Customers");
        }
    }
}
