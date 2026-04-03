package com.agriscope.notification_service.controller;

import com.agriscope.notification_service.dto.DashboardStatsDTO;
import com.agriscope.notification_service.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("GET /dashboard/{farmId} - Should return stats DTO")
    void getDashboardStats_Success() throws Exception {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setAlertDistribution(Map.of(
                "FROST_ALERT", 5L,
                "HEAT_ALERT", 3L
        ));

        stats.setCropVulnerability(Map.of(
                "CORN", 80L
        ));

        when(analyticsService.getDashboardStats("farm1")).thenReturn(stats);

        mockMvc.perform(get("/api/analytics/dashboard/farm1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertDistribution.FROST_ALERT").value(5))
                .andExpect(jsonPath("$.alertDistribution.HEAT_ALERT").value(3))
                .andExpect(jsonPath("$.cropVulnerability.CORN").value(80));

        verify(analyticsService, times(1)).getDashboardStats("farm1");
    }
}