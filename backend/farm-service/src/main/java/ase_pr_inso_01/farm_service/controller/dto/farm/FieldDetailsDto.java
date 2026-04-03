package ase_pr_inso_01.farm_service.controller.dto.farm;

import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.models.enums.GrowthStage;
import ase_pr_inso_01.farm_service.models.enums.SeedType;

import java.util.Date;

public record FieldDetailsDto(
        Integer id,
        FieldStatus status,
        SeedType seedType,
        Date plantedDate,
        Date harvestDate,
        GrowthStage growthStage
) {
}