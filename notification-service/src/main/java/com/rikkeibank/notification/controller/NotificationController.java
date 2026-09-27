package com.rikkeibank.notification.controller;

import com.rikkeibank.notification.dto.NotificationDto;
import com.rikkeibank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications() {
        List<NotificationDto> list = notificationService.getAllNotifications();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationDto>> getNotificationsByCustomer(@PathVariable Long customerId) {
        List<NotificationDto> list = notificationService.getNotificationsByCustomerId(customerId);
        return ResponseEntity.ok(list);
    }

    /**
     * Reactive Server-Sent Events (SSE) endpoint for real-time live notification push to mobile/web
     */
    @GetMapping(value = "/stream/{customerId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NotificationDto> streamNotifications(@PathVariable Long customerId) {
        log.info("SSE Stream connection established for customer {}", customerId);
        return notificationService.getLiveNotificationStream(customerId);
    }
}
