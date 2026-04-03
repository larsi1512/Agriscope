package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.dto.WeatherUpdateDTO;
import com.agriscope.notification_service.model.WeatherDocument;
import com.agriscope.notification_service.repository.WeatherRepository;
import com.agriscope.notification_service.service.WebSocketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherNotificationListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private WeatherRepository weatherRepository;

    @InjectMocks
    private WeatherNotificationListener listener;

    @Test
    @DisplayName("Should ignore weather update if type is NOT 'current'")
    void handleWeather_WrongType_ShouldIgnore() {
        WeatherUpdateDTO dto = new WeatherUpdateDTO();
        dto.setType("forecast_5_days"); // Wrong type
        dto.setUserId("user1");

        listener.handleWeather(dto);

        verifyNoInteractions(webSocketService);
        verifyNoInteractions(weatherRepository);
    }

    @Test
    @DisplayName("Should ignore weather update if userId is missing")
    void handleWeather_NoUserId_ShouldIgnore() {
        WeatherUpdateDTO dto = new WeatherUpdateDTO();
        dto.setType("current");
        dto.setUserId(null);

        listener.handleWeather(dto);

        verifyNoInteractions(webSocketService);
    }

    @Test
    @DisplayName("Should process valid 'current' weather, send WS message and cache to DB")
    void handleWeather_Success() {
        WeatherUpdateDTO dto = new WeatherUpdateDTO();
        dto.setType("current");
        dto.setUserId("user1");
        dto.setFarmId("farm1");
        dto.setLat(45.0);
        dto.setLon(19.0);

        WeatherUpdateDTO.ForecastDTO forecast = new WeatherUpdateDTO.ForecastDTO();
        forecast.setTemperature(25.5);
        forecast.setWeatherCode(1);
        forecast.setTime("2024-05-20 14:00");
        dto.setForecast(List.of(forecast));

        when(weatherRepository.findByFarmId("farm1")).thenReturn(Optional.empty());

        listener.handleWeather(dto);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketService).sendWeatherToUser(eq("farm1"), mapCaptor.capture());

        Map<String, Object> payload = mapCaptor.getValue();
        assertEquals("user1", payload.get("user_id"));
        assertEquals(25.5, payload.get("temp"));
        assertEquals("farm1", payload.get("farm_id"));

        ArgumentCaptor<WeatherDocument> docCaptor = ArgumentCaptor.forClass(WeatherDocument.class);
        verify(weatherRepository).save(docCaptor.capture());

        WeatherDocument savedDoc = docCaptor.getValue();
        assertEquals("farm1", savedDoc.getFarmId());
        assertEquals(25.5, savedDoc.getTemp());
        assertNotNull(savedDoc.getStoredAt());
    }

    @Test
    @DisplayName("Should handle DB exception gracefully (log error, do not throw)")
    void handleWeather_DbError_ShouldNotCrash() {
        WeatherUpdateDTO dto = new WeatherUpdateDTO();
        dto.setType("current");
        dto.setUserId("user1");
        dto.setFarmId("farm1");
        dto.setForecast(List.of(new WeatherUpdateDTO.ForecastDTO()));

        when(weatherRepository.findByFarmId(any())).thenThrow(new RuntimeException("DB Connection failed"));

        assertDoesNotThrow(() -> listener.handleWeather(dto));

        verify(webSocketService).sendWeatherToUser(eq("farm1"), any());
    }

    @Test
    @DisplayName("Should handle missing forecast list gracefully")
    void handleWeather_NoForecastList_ShouldLogWarning() {
        WeatherUpdateDTO dto = new WeatherUpdateDTO();
        dto.setType("current");
        dto.setUserId("user1");
        dto.setForecast(Collections.emptyList());

        listener.handleWeather(dto);

        verifyNoInteractions(webSocketService);
        verifyNoInteractions(weatherRepository);
    }
}