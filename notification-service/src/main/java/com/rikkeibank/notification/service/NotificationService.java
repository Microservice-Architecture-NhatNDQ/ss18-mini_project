package com.rikkeibank.notification.service;

import com.rikkeibank.notification.consumer.TransactionEventConsumer;
import com.rikkeibank.notification.dto.NotificationDto;
import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TransactionEventConsumer eventConsumer;

    public List<NotificationDto> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getNotificationsByCustomerId(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * WebFlux Reactive SSE Flux Stream for real-time live notification push
     */
    public Flux<NotificationDto> getLiveNotificationStream(Long customerId) {
        log.info("Client subscribed to SSE notification stream for customerId: {}", customerId);
        return eventConsumer.getNotificationSink().asFlux()
                .filter(dto -> customerId == null || dto.getCustomerId() == null || dto.getCustomerId().equals(customerId));
    }

    public NotificationDto mapToDto(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .eventType(entity.getEventType())
                .referenceCode(entity.getReferenceCode())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
