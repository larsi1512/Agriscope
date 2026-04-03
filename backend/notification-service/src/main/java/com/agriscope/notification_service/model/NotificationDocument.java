package com.agriscope.notification_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "alerts")
public class NotificationDocument {

    @Id
    private String id;

    @Indexed
    private String farmId;

    @Indexed
    private String userId;

    private String fieldId;


    private String recommendationType;
    private String recommendedSeed;
    private String message;
    private String reasoning;
    @Field("isRead")
    private boolean read = false;
    private LocalDateTime createdAt;

    @Indexed(expireAfter = "7d")
    private LocalDateTime expiryDate;
}