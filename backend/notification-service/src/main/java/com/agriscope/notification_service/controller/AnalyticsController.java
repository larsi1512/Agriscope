package com.agriscope.notification_service.controller;

import com.agriscope.notification_service.dto.DashboardStatsDTO;
import com.agriscope.notification_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard/{farmId}")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(@PathVariable String farmId) {
        return ResponseEntity.ok(analyticsService.getDashboardStats(farmId));
    }
}