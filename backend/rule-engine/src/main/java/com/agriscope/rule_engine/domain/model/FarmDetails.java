package com.agriscope.rule_engine.domain.model;

import com.agriscope.rule_engine.domain.enums.SoilType;
import lombok.Data;

import java.util.Map;

@Data
public class FarmDetails {
    private String farmId;
    private SoilType soilType;
    private Map<String, Double> feedbackFactors;
}