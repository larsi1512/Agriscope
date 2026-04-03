package com.agriscope.rule_engine;

import com.agriscope.rule_engine.config.RabbitMQConfig;
import com.agriscope.rule_engine.domain.model.Recommendation;
import com.agriscope.rule_engine.messaging.RecommendationProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RecommendationProducer recommendationProducer;

    @Test
    void sendRecommendation_ShouldSendToCorrectExchangeAndQueue() {
        Recommendation recommendation = new Recommendation();
        recommendation.setUserId("user1");
        recommendation.setAdvice("IRRIGATE NOW");

        recommendationProducer.sendRecommendation(recommendation);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ALERT_EXCHANGE),
                eq(RabbitMQConfig.ALERT_ROUTING_KEY),
                eq(recommendation)
        );
    }
}