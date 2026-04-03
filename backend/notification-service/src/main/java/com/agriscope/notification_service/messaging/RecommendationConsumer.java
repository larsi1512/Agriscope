package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.config.RabbitMQConfig;
import com.agriscope.notification_service.model.Recommendation;
import com.agriscope.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.ALERT_QUEUE)
    public void handleRecommendation(Recommendation recommendation) {
        log.info("Received recommendation id={}, farmId={}, type={}, fieldId={}",
                recommendation.getId(),
                recommendation.getFarmId(),
                recommendation.getRecommendationType(),
                recommendation.getFieldId());

        try {
            notificationService.processIncomingRecommendation(recommendation);
        } catch (Exception e) {
            log.error("Failed to process recommendation type {}. Data: {}. Error: {}",
                    recommendation.getRecommendationType(), recommendation, e.getMessage());
        }
    }
}