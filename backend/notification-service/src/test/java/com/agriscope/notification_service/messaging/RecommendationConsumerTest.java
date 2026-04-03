package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.model.Recommendation;
import com.agriscope.notification_service.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RecommendationConsumer consumer;

    @Test
    @DisplayName("Should process valid recommendation")
    void handleRecommendation_Success() {
        Recommendation rec = new Recommendation();
        rec.setId("rec1");
        rec.setFarmId("farm1");
        rec.setUserId("user1");

        consumer.handleRecommendation(rec);

        verify(notificationService, times(1)).processIncomingRecommendation(rec);
    }

    @Test
    @DisplayName("Should catch exception from service and log error")
    void handleRecommendation_Exception_ShouldNotThrow() {
        Recommendation rec = new Recommendation();
        rec.setId("rec1");

        doThrow(new RuntimeException("Processing failed")).when(notificationService).processIncomingRecommendation(rec);

        consumer.handleRecommendation(rec);
        verify(notificationService, times(1)).processIncomingRecommendation(rec);
    }
}