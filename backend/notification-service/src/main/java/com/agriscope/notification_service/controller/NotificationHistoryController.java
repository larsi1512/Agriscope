package com.agriscope.notification_service.controller;

import com.agriscope.notification_service.model.NotificationDocument;
import com.agriscope.notification_service.model.WeatherDocument;
import com.agriscope.notification_service.repository.NotificationRepository;
import com.agriscope.notification_service.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationHistoryController {

    private final NotificationRepository notificationRepository;
    private final WeatherRepository weatherRepository;

    @GetMapping("/weather/latest/{farmId}")
    public ResponseEntity<WeatherDocument> getLatestWeather(@PathVariable String farmId) {
        return weatherRepository.findByFarmId(farmId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/alerts/{farmId}")
    public ResponseEntity<List<NotificationDocument>> getFarmAlerts(
            @PathVariable String farmId,
            @RequestParam(required = false, defaultValue = "true") boolean unreadOnly
    ) {
        List<NotificationDocument> alerts;

        if (unreadOnly) {
            alerts = notificationRepository.findByFarmIdAndReadFalseOrderByCreatedAtDesc(farmId);
        } else {
            alerts = notificationRepository.findByFarmIdOrderByCreatedAtDesc(farmId);
        }

        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/alerts/{alertId}/read")
    public void markAsRead(@PathVariable String alertId) {
        notificationRepository.findById(alertId).ifPresent(alert -> {
            alert.setRead(true);
            notificationRepository.save(alert);
        });
    }
}