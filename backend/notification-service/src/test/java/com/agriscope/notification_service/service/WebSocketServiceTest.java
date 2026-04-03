package com.agriscope.notification_service.service;

import com.agriscope.notification_service.dto.WeatherUpdateDTO;
import com.agriscope.notification_service.model.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    @Test
    @DisplayName("Should send ALERT to correct topic")
    void sendAlertToFarm_Success() {
        String farmId = "farm1";
        Recommendation rec = new Recommendation();
        rec.setId("rec1");

        webSocketService.sendAlertToFarm(farmId, rec);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/alerts/farm1"), eq(rec));
    }

    @Test
    @DisplayName("Should send RECOMMENDATION to correct topic")
    void sendRecommendationToFarm_Success() {
        String farmId = "farm2";
        Recommendation rec = new Recommendation();
        rec.setId("rec2");

        webSocketService.sendRecommendationToFarm(farmId, rec);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/recommendations/farm2"), eq(rec));
    }

    @Test
    @DisplayName("Should send WEATHER to correct topic")
    void sendWeatherToUser_Success() {
        String farmId = "farm3";
        Map<String, Object> payload = Map.of("temp", 25.0);

        webSocketService.sendWeatherToUser(farmId, payload);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/weather/farm3"), eq(payload));
    }

    @Test
    @DisplayName("Should handle exception gracefully if messaging fails")
    void sendAlert_Exception_ShouldNotThrow() {
        String farmId = "farm1";
        Recommendation rec = new Recommendation();

        doThrow(new RuntimeException("Connection lost"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> webSocketService.sendAlertToFarm(farmId, rec));

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should handle exception gracefully if messaging fails")
    void sendRecommendation_Exception_ShouldNotThrow() {
        String farmId = "farm1";
        Recommendation rec = new Recommendation();

        doThrow(new RuntimeException("Connection lost"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> webSocketService.sendRecommendationToFarm(farmId, rec));

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should handle exception gracefully if messaging fails")
    void sendWeather_Exception_ShouldNotThrow() {
        String farmId = "farm1";
        WeatherUpdateDTO weather = new WeatherUpdateDTO();

        doThrow(new RuntimeException("Connection lost"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> webSocketService.sendWeatherToUser(farmId, weather));

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }
}