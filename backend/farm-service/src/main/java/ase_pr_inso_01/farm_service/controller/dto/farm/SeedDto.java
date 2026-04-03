package ase_pr_inso_01.farm_service.controller.dto.farm;

import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.Recommendation;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
import ase_pr_inso_01.farm_service.models.enums.SoilType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeedDto {
    private String id;
    private SeedType seedType;
    private String displayName;
    private String scientificName;

    private Double minTemperature;
    private Double maxTemperature;
    private Double heatStressTemperature;

    private Double waterRequirement;
    private Double heavyRainThreshold;
    private Double minSoilMoisture;
    private Double allowedWaterDeficit;

    private Integer daysToYoung;
    private Integer daysToMature;
    private Integer daysToReady;

    private Double diseaseRiskMinTemp;
    private Double diseaseRiskMaxTemp;
    private Double diseaseRainThreshold;

    private Double maxWindTolerance;

    private Double seedCoefficient;
}