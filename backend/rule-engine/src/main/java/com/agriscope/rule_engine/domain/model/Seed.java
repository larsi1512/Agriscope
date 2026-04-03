package com.agriscope.rule_engine.domain.model;

import com.agriscope.rule_engine.domain.enums.SeedType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Seed {
    private SeedType seedType;
    private String displayName;
    private String scientificName;

    private Double minTemperature;
    private Double maxTemperature;

    private Double minSoilMoisture;
    private Double waterRequirement;

    private Double frostRiskTemperature;
    private Double heatStressTemperature;
    private Double heavyRainThreshold;
    private Double seedCoefficient;
    private Double allowedWaterDeficit;

    private Double diseaseRiskMinTemp;
    private Double diseaseRiskMaxTemp;
    private Double diseaseRainThreshold;
    private Double maxWindTolerance;

    private Map<String, Double> ruleParams = new HashMap<>();
    public double getParam(String key, double defaultValue) {
        if (ruleParams != null && ruleParams.containsKey(key)) {
            return ruleParams.get(key);
        }
        return defaultValue;
    }


    public Seed copy() {
        Seed newSeed = new Seed();
        newSeed.setSeedType(this.seedType);
        newSeed.setDisplayName(this.displayName);
        newSeed.setScientificName(this.scientificName);

        newSeed.setMinTemperature(this.minTemperature);
        newSeed.setMaxTemperature(this.maxTemperature);
        newSeed.setWaterRequirement(this.waterRequirement);
        newSeed.setHeatStressTemperature(this.heatStressTemperature);
        newSeed.setHeavyRainThreshold(this.heavyRainThreshold);
        newSeed.setSeedCoefficient(this.seedCoefficient);
        newSeed.setMinSoilMoisture(this.minSoilMoisture);
        newSeed.setAllowedWaterDeficit(this.allowedWaterDeficit);
        newSeed.setDiseaseRiskMinTemp(this.diseaseRiskMinTemp);
        newSeed.setDiseaseRiskMaxTemp(this.diseaseRiskMaxTemp);
        newSeed.setDiseaseRainThreshold(this.diseaseRainThreshold);
        newSeed.setMaxWindTolerance(this.maxWindTolerance);

        if (this.ruleParams != null) {
            newSeed.setRuleParams(new java.util.HashMap<>(this.ruleParams));
        } else {
            newSeed.setRuleParams(new java.util.HashMap<>());
        }

        return newSeed;
    }
}