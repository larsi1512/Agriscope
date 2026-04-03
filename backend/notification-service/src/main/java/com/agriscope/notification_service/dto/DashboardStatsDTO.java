package com.agriscope.notification_service.dto;

import java.util.Map;
import lombok.Data;

@Data
public class DashboardStatsDTO {
    private Map<String, Long> alertDistribution;

    private Map<String, Long> cropVulnerability;

    private Map<String, Long> waterSavings;
}