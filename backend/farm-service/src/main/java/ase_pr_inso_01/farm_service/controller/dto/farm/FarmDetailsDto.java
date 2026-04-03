package ase_pr_inso_01.farm_service.controller.dto.farm;

import ase_pr_inso_01.farm_service.models.Recommendation;
import ase_pr_inso_01.farm_service.models.enums.SoilType;

public record FarmDetailsDto(
    String id,
    String name,
    float latitude,
    float longitude,
    SoilType soilType,
    FieldDetailsDto[] fields,
    Recommendation[] recommendations, //TODO: Change to dto
    String userId,
    java.util.Map<String, Double> feedbackFactors
) {
}
