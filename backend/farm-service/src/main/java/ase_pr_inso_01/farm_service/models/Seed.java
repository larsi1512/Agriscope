package ase_pr_inso_01.farm_service.models;

import ase_pr_inso_01.farm_service.models.enums.SeedType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "seed")
@Getter
@Setter
public class Seed {
    @Id
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
    private Double seedCoefficient;

    private Integer daysToYoung;
    private Integer daysToMature;
    private Integer daysToReady;
    private Double diseaseRiskMinTemp;
    private Double diseaseRiskMaxTemp;
    private Double diseaseRainThreshold;

    private Double maxWindTolerance;
}