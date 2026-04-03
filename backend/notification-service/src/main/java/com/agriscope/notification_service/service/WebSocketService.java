package com.agriscope.notification_service.service;


import com.agriscope.notification_service.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendAlertToFarm(String farmId, Recommendation recommendation) {
        try {
            String destination = "/topic/alerts/" + farmId;
            messagingTemplate.convertAndSend(destination, recommendation);
            log.info("Sent WebSocket ALERT to farm: {}", farmId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket alert to farm: {}. Error:{}", farmId, e.getMessage());
        }
    }

    public void sendRecommendationToFarm(String farmId, Recommendation recommendation) {
        try {
            String destination = "/topic/recommendations/" + farmId;
            messagingTemplate.convertAndSend(destination, recommendation);
            log.info("Sent WebSocket RECOMMENDATION to farm: {}", farmId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket recommendation to farm: {}. Error:{}", farmId, e.getMessage());
        }
    }

    public void sendWeatherToUser(String farmId, Object payload) {
        try {
            String destination = "/topic/weather/" + farmId;
            messagingTemplate.convertAndSend(destination, payload);
            log.info("Sent WebSocket WEATHER to farm: {}", farmId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket weather to farm: {}. Error:{}", farmId, e.getMessage());
        }
    }
}