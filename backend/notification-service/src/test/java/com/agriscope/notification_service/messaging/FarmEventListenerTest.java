package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FarmEventListener listener;

    @Test
    @DisplayName("Should clear cache when event is 'field.harvested'")
    void handleHarvestEvent_Success() {
        Map<String, Object> message = new HashMap<>();
        message.put("event", "field.harvested");
        message.put("farmId", "farm1");
        message.put("fieldId", "field1");

        listener.handleHarvestEvent(message);

        verify(notificationService).clearCacheForField("farm1", "field1");
    }

    @Test
    @DisplayName("Should ignore unrelated events")
    void handleHarvestEvent_WrongEvent_ShouldIgnore() {
        Map<String, Object> message = new HashMap<>();
        message.put("event", "field.updated"); // Wrong event
        message.put("farmId", "farm1");

        listener.handleHarvestEvent(message);

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should handle missing keys or nulls gracefully")
    void handleHarvestEvent_InvalidData_ShouldNotCrash() {
        Map<String, Object> message = new HashMap<>();
        // Missing event key

        listener.handleHarvestEvent(message);
        verifyNoInteractions(notificationService);
    }
}