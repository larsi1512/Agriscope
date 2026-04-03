package com.agriscope.notification_service.service;

import com.agriscope.notification_service.model.NotificationDocument;
import com.agriscope.notification_service.model.Recommendation;
import com.agriscope.notification_service.repository.NotificationRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebSocketService webSocketService;

    private final EmailService emailService;

    private final ConcurrentHashMap<String, Long> pendingAcks = new ConcurrentHashMap<>();

    private final EmailTemplateService emailTemplateService;

    private final Cache<String, Recommendation> lastSentCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    private static final double TEMP_CHANGE_THRESHOLD = 2.0;
    private static final double DEFICIT_CHANGE_THRESHOLD = 2.0;

    private static final String[] ALERT_TYPES = {
            "FROST_ALERT",
            "HEAT_ALERT",
            "STORM_ALERT",
            "SAFETY_ALERT",
            "IRRIGATE_NOW"
    };

    private static final String[] RECOMMENDATION_TYPES = {
            "IRRIGATE_SOON",
            "DELAY_IRRIGATION",
            "MONITOR_CONDITIONS",
            "CONTINUE_NORMAL",
            "DELAY_OPERATIONS",
            "DISEASE_PREVENTION",
            "NUTRIENT_CHECK",
            "PLANNING_ALERT",
            "HEAT_STRESS_PREVENTION",
            "PEST_RISK",
            "READY_TO_HARVEST"
    };
    private final NotificationRepository notificationRepository;

    public void processIncomingRecommendation(Recommendation newRec) {
        String farmId = newRec.getFarmId();
        if (farmId == null) {
            log.warn("Received recommendation without farmId");
            return;
        }

        log.info("Field id: ", newRec.getFieldId());
        String fieldIdentifier = newRec.getFieldId() != null ? newRec.getFieldId() : "FARM_WIDE";
        String uniqueKey = String.format("%s_%s_%s_%s",
                farmId,
                fieldIdentifier,
                newRec.getRecommendationType(),
                newRec.getRecommendedSeed());

        Recommendation lastRec = lastSentCache.getIfPresent(uniqueKey);

        if (lastRec == null) {
            sendAndCache(uniqueKey, newRec);
            return;
        }

        if (isSignificantChange(lastRec, newRec)) {
            log.info("Significant change detected, sending update for {}.", newRec.getRecommendationType());
            sendAndCache(uniqueKey, newRec);
        } else {
            log.info("Duplicate alert suppressed (insignificant change).");
        }
    }

    private void sendAndCache(String key, Recommendation rec) {
        if (isAlertType(rec.getRecommendationType())) {
            webSocketService.sendAlertToFarm(rec.getFarmId(), rec);

            String fieldName = rec.getFieldName(); // Get from recommendation
            String htmlEmailBody = emailTemplateService.buildAlertEmailHtml(rec, fieldName);
            String emailSubject = rec.getRecommendationType();

            pendingAcks.put(rec.getId(), System.currentTimeMillis());
            emailService.sendAlertEmail(rec.getEmail(), emailSubject, htmlEmailBody);
            log.info("Sent HTML alert email for farm: {}", rec.getFarmId());
        } else if (isRecommendationType(rec.getRecommendationType())) {
            webSocketService.sendRecommendationToFarm(rec.getFarmId(), rec);
            log.info("Sent as RECOMMENDATION for farm: {}", rec.getFarmId());
        } else {
            log.warn("Unknown recommendation type: {}, sending as recommendation", rec.getRecommendationType());
            webSocketService.sendRecommendationToFarm(rec.getFarmId(), rec);
        }

        try {
            NotificationDocument doc = new NotificationDocument();
            doc.setId(rec.getId());
            doc.setFarmId(rec.getFarmId());
            doc.setFieldId(rec.getFieldId());
            doc.setUserId(rec.getUserId());
            doc.setRecommendationType(rec.getRecommendationType());
            doc.setRecommendedSeed(rec.getRecommendedSeed());
            doc.setMessage(rec.getAdvice());
            doc.setReasoning(rec.getReasoning());
            doc.setCreatedAt(LocalDateTime.now());
            doc.setExpiryDate(LocalDateTime.now().plusDays(7));
            doc.setRead(false);

            notificationRepository.save(doc);
            log.info("Alert saved to DB for farm: {}", rec.getFarmId());
        } catch (Exception e) {
            log.error("Failed to save alert to DB", e);
        }

        lastSentCache.put(key, rec);
    }

    public void markAsDelivered(String messageId) {
        if (pendingAcks.containsKey(messageId)) {
            pendingAcks.remove(messageId);
            log.info("Message {} confirmed delivered via WebSocket (User is Online).", messageId);
        }
    }


    @Scheduled(fixedRate = 10000)
    public void checkFailedDeliveries() {
        long now = System.currentTimeMillis();
        long timeout = 5 * 60 * 1000;

        pendingAcks.forEach((msgId, timestamp) -> {
            if (now - timestamp > timeout) {
                log.debug("Ack timeout for msgId: {}. (Email was already sent).", msgId);
                pendingAcks.remove(msgId);
            }
        });
    }

    private boolean isAlertType(String type) {
        for (String alertType : ALERT_TYPES) {
            if (alertType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecommendationType(String type) {
        for (String recType : RECOMMENDATION_TYPES) {
            if (recType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSignificantChange(Recommendation oldRec, Recommendation newRec) {
        String type = newRec.getRecommendationType();

        if (type.equals("MONITOR_CONDITIONS") ||
                type.equals("CONTINUE_NORMAL") ||
                type.equals("READY_TO_HARVEST") ||
                type.equals("DISEASE_PREVENTION") ||
                type.equals("PEST_RISK") ||
                type.equals("NUTRIENT_CHECK") ||
                type.equals("PLANNING_ALERT") ||
                type.equals("HEAT_STRESS_PREVENTION")) {
            return false;
        }

        double oldValue = getRelevantMetricValue(oldRec, type);
        double newValue = getRelevantMetricValue(newRec, type);
        double diff = Math.abs(newValue - oldValue);

        if (type.equals("FROST_ALERT") || type.equals("HEAT_ALERT")) {
            return diff >= TEMP_CHANGE_THRESHOLD;
        }

        if (type.equals("IRRIGATE_NOW") || type.equals("IRRIGATE_SOON") || type.equals("DELAY_IRRIGATION")) {
            return diff >= DEFICIT_CHANGE_THRESHOLD;
        }

        return false;
    }

    private double getRelevantMetricValue(Recommendation rec, String type) {
        if (rec.getMetrics() == null) return 0.0;

        if (type.equals("FROST_ALERT") || type.equals("HEAT_ALERT")) {
            return getMetricValue(rec, "temperature");
        }

        if (type.equals("IRRIGATE_NOW") || type.equals("IRRIGATE_SOON") || type.equals("DELAY_IRRIGATION")) {
            return getMetricValue(rec, "deficit_amount");
        }

        return 0.0;
    }

    private double getMetricValue(Recommendation rec, String key) {
        if (rec.getMetrics().containsKey(key)) {
            Object obj = rec.getMetrics().get(key);
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            }
        }
        return 0.0;
    }

    public void clearCacheForField(String farmId, String fieldId) {
        String prefix = farmId + "_" + fieldId + "_";
        java.util.List<String> keysToRemove = new java.util.ArrayList<>();

        lastSentCache.asMap().keySet().forEach(key -> {
            if (key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        });

        if (!keysToRemove.isEmpty()) {
            lastSentCache.invalidateAll(keysToRemove);
            log.info("Cleared {} cached notifications for farm {} field {} due to Harvest.",
                    keysToRemove.size(), farmId, fieldId);
        }
    }
}