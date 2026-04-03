package com.agriscope.notification_service.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class Recommendation {
    private String id;
    private String email;
    private String userId;
    private String farmId;
    private String fieldId;
    private String fieldName;

    private String recommendedSeed;
    private String recommendationType;

    private String advice;
    private String reasoning;
    private LocalDateTime weatherTimestamp;
    private Map<String, Object> metrics = new HashMap<>();

}