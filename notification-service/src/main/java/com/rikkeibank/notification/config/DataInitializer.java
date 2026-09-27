package com.rikkeibank.notification.config;

import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final NotificationRepository notificationRepository;

    @Override
    public void run(String... args) {
        if (notificationRepository.count() == 0) {
            log.info("Seeding initial notifications...");
            notificationRepository.save(Notification.builder()
                    .customerId(1L)
                    .title("Chào mừng đến với RikkeiBank Digital")
                    .message("Tài khoản thanh toán 1000123456 của Quý khách đã được kích hoạt thành công.")
                    .eventType("ACCOUNT_ACTIVATED")
                    .referenceCode("INIT-001")
                    .amount(BigDecimal.ZERO)
                    .status("READ")
                    .build());

            notificationRepository.save(Notification.builder()
                    .customerId(2L)
                    .title("Chào mừng đến với RikkeiBank Digital")
                    .message("Tài khoản thanh toán 1000654321 của Quý khách đã được kích hoạt thành công.")
                    .eventType("ACCOUNT_ACTIVATED")
                    .referenceCode("INIT-002")
                    .amount(BigDecimal.ZERO)
                    .status("READ")
                    .build());
        }
    }
}
