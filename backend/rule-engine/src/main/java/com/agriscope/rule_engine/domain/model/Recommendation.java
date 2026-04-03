package com.agriscope.rule_engine.domain.model;

import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.enums.RecommendationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class Recommendation {
    private String id;
    private String userId;
    private String email;
    private String farmId;
    private String fieldId;

    private SeedType recommendedSeed;
    private RecommendationType recommendationType;
    private String advice;
    private String reasoning;
    private LocalDateTime weatherTimestamp;
    private Map<String, Object> metrics = new HashMap<>();

    private boolean read = false;
    private boolean dismissed = false;
    private LocalDateTime readAt;
    private LocalDateTime dismissedAt;

    public Recommendation() {
        this.id = java.util.UUID.randomUUID().toString();
    }
}