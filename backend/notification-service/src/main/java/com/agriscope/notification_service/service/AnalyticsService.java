package com.agriscope.notification_service.service;

import com.agriscope.notification_service.dto.DashboardStatsDTO;
import com.agriscope.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final NotificationRepository notificationRepo;

    public DashboardStatsDTO getDashboardStats(String farmId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        List<NotificationRepository.StatResult> alerts = notificationRepo.countByRecommendationType(farmId);
        Map<String, Long> alertMap = convertToMap(alerts);

        stats.setAlertDistribution(alertMap);

        List<NotificationRepository.StatResult> crops = notificationRepo.countCriticalAlertsBySeed(farmId);
        stats.setCropVulnerability(convertToMap(crops));

        Map<String, Long> savings = new java.util.HashMap<>();

        savings.put("actionsTaken", alertMap.getOrDefault("IRRIGATE_NOW", 0L));

        savings.put("actionsSaved", alertMap.getOrDefault("DELAY_IRRIGATION", 0L));

        stats.setWaterSavings(savings);

        return stats;
    }

    private Map<String, Long> convertToMap(List<NotificationRepository.StatResult> list) {
        return list.stream()
                .filter(res -> res._id != null)
                .collect(Collectors.toMap(res -> res._id, res -> res.count));
    }
}