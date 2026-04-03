package com.agriscope.notification_service.service;

import com.agriscope.notification_service.dto.DashboardStatsDTO;
import com.agriscope.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private NotificationRepository notificationRepo;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Should correctly calculate dashboard stats and water savings")
    void getDashboardStats_Success() {
        String farmId = "farm1";

        List<NotificationRepository.StatResult> alertStats = new ArrayList<>();
        alertStats.add(createStat("IRRIGATE_NOW", 10L));
        alertStats.add(createStat("DELAY_IRRIGATION", 5L));

        List<NotificationRepository.StatResult> cropStats = new ArrayList<>();
        cropStats.add(createStat("CORN", 8L));
        cropStats.add(createStat("WHEAT", 2L));

        when(notificationRepo.countByRecommendationType(farmId)).thenReturn(alertStats);
        when(notificationRepo.countCriticalAlertsBySeed(farmId)).thenReturn(cropStats);

        DashboardStatsDTO result = analyticsService.getDashboardStats(farmId);

        assertNotNull(result);

        Map<String, Long> dist = result.getAlertDistribution();
        assertEquals(10L, dist.get("IRRIGATE_NOW"));

        Map<String, Long> vuln = result.getCropVulnerability();
        assertEquals(8L, vuln.get("CORN"));
        assertEquals(2L, vuln.get("WHEAT"));

        Map<String, Long> savings = result.getWaterSavings();
        assertEquals(10L, savings.get("actionsTaken"));
        assertEquals(5L, savings.get("actionsSaved"));
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void getDashboardStats_EmptyData() {
        String farmId = "farmEmpty";
        when(notificationRepo.countByRecommendationType(farmId)).thenReturn(new ArrayList<>());
        when(notificationRepo.countCriticalAlertsBySeed(farmId)).thenReturn(new ArrayList<>());

        DashboardStatsDTO result = analyticsService.getDashboardStats(farmId);

        assertNotNull(result);
        assertTrue(result.getAlertDistribution().isEmpty());
        assertTrue(result.getCropVulnerability().isEmpty());

        assertEquals(0L, result.getWaterSavings().get("actionsTaken"));
        assertEquals(0L, result.getWaterSavings().get("actionsSaved"));
    }

    private NotificationRepository.StatResult createStat(String id, Long count) {
        NotificationRepository.StatResult stat = new NotificationRepository.StatResult();
        stat._id = id;
        stat.count = count;
        return stat;
    }
}