package com.agriscope.notification_service.service;

import com.agriscope.notification_service.model.NotificationDocument;
import com.agriscope.notification_service.model.Recommendation;
import com.agriscope.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(emailTemplateService.buildAlertEmailHtml(any(), any()))
                .thenReturn("<html><body>Mock Email</body></html>");
    }


    @Test
    @DisplayName("Should process new CRITICAL alert: Save to DB, Send WS, Send Email, Cache it")
    void process_CriticalAlert_FirstTime() {
        Recommendation rec = createRecommendation("rec1", "FROST_ALERT", -2.0, "temperature");
        rec.setUserId("user1");
        rec.setEmail("test@test.com");

        notificationService.processIncomingRecommendation(rec);

        verify(notificationRepository, times(1)).save(any(NotificationDocument.class));
        verify(webSocketService, times(1)).sendAlertToFarm(eq("farm1"), eq(rec));
        verify(emailService, times(1)).sendAlertEmail(eq("test@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should process INFO recommendation: Save to DB, Send WS, NO Email")
    void process_InfoRecommendation_FirstTime() {
        Recommendation rec = createRecommendation("rec2", "MONITOR_CONDITIONS", 0.0, "none");
        rec.setUserId("user1");

        notificationService.processIncomingRecommendation(rec);

        verify(notificationRepository, times(1)).save(any(NotificationDocument.class));
        verify(webSocketService, times(1)).sendRecommendationToFarm(eq("farm1"), eq(rec));
        verify(emailService, never()).sendAlertEmail(anyString(), anyString(), anyString());
    }

    // cache logic

    @Test
    @DisplayName("Should IGNORE duplicate alert if metrics haven't changed significantly")
    void process_DuplicateAlert_ShouldIgnore() {
        Recommendation rec1 = createRecommendation("rec1", "FROST_ALERT", -2.0, "temperature");
        notificationService.processIncomingRecommendation(rec1);

        Recommendation rec2 = createRecommendation("rec2", "FROST_ALERT", -2.5, "temperature");
        notificationService.processIncomingRecommendation(rec2);

        verify(notificationRepository, times(1)).save(any(NotificationDocument.class));
        verify(emailService, times(1)).sendAlertEmail(anyString(), anyString(), anyString());
    }


    @Test
    @DisplayName("Should PROCESS second alert if Temperature change is SIGNIFICANT (> 2.0)")
    void process_SignificantTempChange_ShouldProcess() {
        Recommendation rec1 = createRecommendation("rec1", "FROST_ALERT", -2.0, "temperature");
        notificationService.processIncomingRecommendation(rec1);

        Recommendation rec2 = createRecommendation("rec2", "FROST_ALERT", -5.0, "temperature");
        notificationService.processIncomingRecommendation(rec2);

        verify(notificationRepository, times(2)).save(any(NotificationDocument.class));
        verify(emailService, times(2)).sendAlertEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should PROCESS second alert if Water Deficit change is SIGNIFICANT (> 2.0)")
    void process_SignificantDeficitChange_ShouldProcess() {
        Recommendation rec1 = createRecommendation("rec1", "IRRIGATE_NOW", 10.0, "deficit_amount");
        notificationService.processIncomingRecommendation(rec1);

        Recommendation rec2 = createRecommendation("rec2", "IRRIGATE_NOW", 13.0, "deficit_amount");
        notificationService.processIncomingRecommendation(rec2);

        verify(notificationRepository, times(2)).save(any(NotificationDocument.class));
    }


    @Test
    @DisplayName("Should clear cache and allow new alert after Harvest event")
    void clearCache_ShouldAllowNewAlert() {
        Recommendation rec1 = createRecommendation("rec1", "FROST_ALERT", -2.0, "temperature");
        notificationService.processIncomingRecommendation(rec1);

        notificationService.processIncomingRecommendation(rec1);
        verify(notificationRepository, times(1)).save(any()); // Still 1

        notificationService.clearCacheForField("farm1", "field1");
        notificationService.processIncomingRecommendation(rec1);

        verify(notificationRepository, times(2)).save(any());
    }


    @Test
    @DisplayName("Edge Case: Metrics Missing or Null should not crash")
    void process_NullMetrics_ShouldHandleGracefully() {
        Recommendation rec = new Recommendation();
        rec.setId("rec_empty");
        rec.setFarmId("farm1");
        rec.setFieldId("field1");
        rec.setRecommendationType("FROST_ALERT");
        rec.setEmail("test@test.com");
        rec.setMetrics(null);

        notificationService.processIncomingRecommendation(rec);

        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Edge Case: Different Recommendation Type on same field should pass")
    void process_DifferentTypes_ShouldNotBlockEachOther() {
        Recommendation rec1 = createRecommendation("rec1", "FROST_ALERT", -2.0, "temperature");
        notificationService.processIncomingRecommendation(rec1);

        Recommendation rec2 = createRecommendation("rec2", "HEAT_ALERT", 35.0, "temperature");
        notificationService.processIncomingRecommendation(rec2);

        verify(notificationRepository, times(2)).save(any());
    }


    private Recommendation createRecommendation(String id, String type, double metricVal, String metricKey) {
        Recommendation rec = new Recommendation();
        rec.setId(id);
        rec.setFarmId("farm1");
        rec.setFieldId("field1");
        rec.setRecommendationType(type);
        rec.setAdvice("Test Advice");
        rec.setEmail("default-test@agriscope.com");
        rec.setWeatherTimestamp(LocalDateTime.now());

        Map<String, Object> metrics = new HashMap<>();
        metrics.put(metricKey, metricVal);
        rec.setMetrics(metrics);

        return rec;
    }
}