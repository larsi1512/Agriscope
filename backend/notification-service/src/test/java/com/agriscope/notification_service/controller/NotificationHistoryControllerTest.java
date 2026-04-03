package com.agriscope.notification_service.controller;

import com.agriscope.notification_service.model.NotificationDocument;
import com.agriscope.notification_service.model.WeatherDocument;
import com.agriscope.notification_service.repository.NotificationRepository;
import com.agriscope.notification_service.repository.WeatherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationHistoryController.class)
class NotificationHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private WeatherRepository weatherRepository;


    @Test
    @DisplayName("GET /weather/latest - Should return 200 and weather data when found")
    void getLatestWeather_Found() throws Exception {
        WeatherDocument weather = new WeatherDocument();
        weather.setFarmId("farm1");
        weather.setTemp(25.5);

        when(weatherRepository.findByFarmId("farm1")).thenReturn(Optional.of(weather));

        mockMvc.perform(get("/api/notifications/weather/latest/farm1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmId").value("farm1"))
                .andExpect(jsonPath("$.temp").value(25.5));

        verify(weatherRepository, times(1)).findByFarmId("farm1");
    }

    @Test
    @DisplayName("GET /weather/latest - Should return 204 No Content when not found")
    void getLatestWeather_NotFound() throws Exception {
        when(weatherRepository.findByFarmId("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications/weather/latest/unknown"))
                .andExpect(status().isNoContent());

        verify(weatherRepository, times(1)).findByFarmId("unknown");
    }


    @Test
    @DisplayName("GET /alerts - Should return unread alerts by default (unreadOnly=true)")
    void getFarmAlerts_DefaultUnreadOnly() throws Exception {
        NotificationDocument notif = new NotificationDocument();
        notif.setFarmId("farm1");
        notif.setRead(false);
        notif.setMessage("Test Alert");

        when(notificationRepository.findByFarmIdAndReadFalseOrderByCreatedAtDesc("farm1"))
                .thenReturn(Arrays.asList(notif));

        mockMvc.perform(get("/api/notifications/alerts/farm1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Test Alert"))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(notificationRepository, times(1)).findByFarmIdAndReadFalseOrderByCreatedAtDesc("farm1");
        verify(notificationRepository, never()).findByFarmIdOrderByCreatedAtDesc(anyString());
    }

    @Test
    @DisplayName("GET /alerts - Should return all alerts when unreadOnly=false")
    void getFarmAlerts_All() throws Exception {
        when(notificationRepository.findByFarmIdOrderByCreatedAtDesc("farm1"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/notifications/alerts/farm1")
                        .param("unreadOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(notificationRepository, times(1)).findByFarmIdOrderByCreatedAtDesc("farm1");
        verify(notificationRepository, never()).findByFarmIdAndReadFalseOrderByCreatedAtDesc(anyString());
    }


    @Test
    @DisplayName("PUT /alerts/{id}/read - Should mark notification as read and save")
    void markAsRead_Success() throws Exception {
        NotificationDocument notif = new NotificationDocument();
        notif.setId("alert123");
        notif.setRead(false);

        when(notificationRepository.findById("alert123")).thenReturn(Optional.of(notif));

        mockMvc.perform(put("/api/notifications/alerts/alert123/read"))
                .andExpect(status().isOk());

        verify(notificationRepository, times(1)).save(argThat(argument ->
                argument.getId().equals("alert123") && argument.isRead()
        ));
    }

    @Test
    @DisplayName("PUT /alerts/{id}/read - Should do nothing if ID not found")
    void markAsRead_NotFound() throws Exception {
        when(notificationRepository.findById("invalid-id")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/notifications/alerts/invalid-id/read"))
                .andExpect(status().isOk());

        verify(notificationRepository, never()).save(any());
    }
}