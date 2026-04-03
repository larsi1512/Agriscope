package com.agriscope.rule_engine.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyAnalysis {
    private double totalEt0;
    private double totalRain;
    private double avgWindSpeed;
    private double currentSoilMoisture;
}