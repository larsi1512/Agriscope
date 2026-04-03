package com.agriscope.rule_engine.messaging;


import com.agriscope.rule_engine.domain.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.agriscope.rule_engine.config.RabbitMQConfig.ALERT_EXCHANGE;
import static com.agriscope.rule_engine.config.RabbitMQConfig.ALERT_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationProducer {

    private final RabbitTemplate rabbitTemplate;


    public void sendRecommendation(Recommendation recommendation) {
        try {
            rabbitTemplate.convertAndSend(
                    ALERT_EXCHANGE,
                    ALERT_ROUTING_KEY,
                    recommendation
            );
            log.info("Sent recommendation to alert service: {} for user {}",
                    recommendation.getAdvice(), recommendation.getUserId());
        } catch (Exception e) {
            log.error("Failed to send recommendation to alert service: {}", e.getMessage(), e);
        }
    }
}