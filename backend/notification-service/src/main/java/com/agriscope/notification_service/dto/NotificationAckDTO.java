package com.agriscope.notification_service.dto;

import lombok.Data;

@Data
public class NotificationAckDTO {
    private String recommendationId;
    private String farmId;
    private String status;
}
