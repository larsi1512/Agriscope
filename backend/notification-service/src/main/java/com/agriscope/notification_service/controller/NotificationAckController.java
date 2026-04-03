package com.agriscope.notification_service.controller;

import com.agriscope.notification_service.dto.NotificationAckDTO;
import com.agriscope.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationAckController {

    private final NotificationService notificationService;

    @MessageMapping("/notification/ack")
    public void handleAcknowledgment(NotificationAckDTO ack) {
        log.info("ACK received for recommendation {} from farm {}", ack.getRecommendationId(), ack.getFarmId());

        notificationService.markAsDelivered(ack.getRecommendationId());
    }
}
