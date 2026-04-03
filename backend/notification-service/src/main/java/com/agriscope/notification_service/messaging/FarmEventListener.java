package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FarmEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification_harvest_queue")
    public void handleHarvestEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("event");

            if ("field.harvested".equals(eventType)) {
                String farmId = (String) message.get("farmId");
                String fieldId = (String) message.get("fieldId");

                log.info("Received Harvest Event for farm {} field {}. Clearing cache...", farmId, fieldId);

                notificationService.clearCacheForField(farmId, fieldId);
            }
        } catch (Exception e) {
            log.error("Error processing harvest event", e);
        }
    }
}