package com.rikkeibank.notification.consumer;

import com.rikkeibank.notification.dto.NotificationDto;
import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.event.TransactionEvent;
import com.rikkeibank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationRepository notificationRepository;
    private final Sinks.Many<NotificationDto> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    public Sinks.Many<NotificationDto> getNotificationSink() {
        return notificationSink;
    }

    @KafkaListener(topics = "transaction-events", groupId = "notification-service-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        log.info("[Event-Driven Kafka Consumer] Received event {} for reference {}", event.getEventType(), event.getReferenceCode());

        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String amountFormatted = (event.getAmount() != null) ? vnFormat.format(event.getAmount()) + " VND" : "0 VND";

        String title;
        String message;

        switch (event.getEventType()) {
            case "SAGA_TRANSFER_COMPLETED" -> {
                title = "Biến động số dư - Chuyển tiền thành công";
                message = String.format("Tài khoản %s đã chuyển thành công %s đến tài khoản %s. Mã GD: %s",
                        event.getFromAccountNumber(), amountFormatted, event.getToAccountNumber(), event.getReferenceCode());
            }
            case "SAGA_TRANSFER_COMPENSATED" -> {
                title = "Cảnh báo GD - Đã hoàn tiền (Compensated)";
                message = String.format("Giao dịch %s chuyển %s đến %s không thành công. Hệ thống đã hoàn tiền về tài khoản %s.",
                        event.getReferenceCode(), amountFormatted, event.getToAccountNumber(), event.getFromAccountNumber());
            }
            case "SAGA_TRANSFER_FAILED" -> {
                title = "Giao dịch chuyển tiền thất bại";
                message = String.format("Giao dịch %s số tiền %s thất bại: %s",
                        event.getReferenceCode(), amountFormatted, event.getMessage());
            }
            default -> {
                title = "Thông báo giao dịch RikkeiBank";
                message = String.format("Trạng thái: %s - Mã GD: %s", event.getEventType(), event.getReferenceCode());
            }
        }

        Notification entity = Notification.builder()
                .customerId(event.getCustomerId())
                .title(title)
                .message(message)
                .eventType(event.getEventType())
                .referenceCode(event.getReferenceCode())
                .amount(event.getAmount())
                .status("UNREAD")
                .build();

        Notification saved = notificationRepository.save(entity);
        log.info("Notification saved with ID: {} for customer ID: {}", saved.getId(), saved.getCustomerId());

        NotificationDto dto = NotificationDto.builder()
                .id(saved.getId())
                .customerId(saved.getCustomerId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .eventType(saved.getEventType())
                .referenceCode(saved.getReferenceCode())
                .amount(saved.getAmount())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();

        // Emit to reactive SSE sink for real-time push to mobile apps / frontend
        notificationSink.tryEmitNext(dto);
    }
}
