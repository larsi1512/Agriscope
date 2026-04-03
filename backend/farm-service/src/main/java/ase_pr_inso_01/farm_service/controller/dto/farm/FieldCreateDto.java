package ase_pr_inso_01.farm_service.controller.dto.farm;

import ase_pr_inso_01.farm_service.models.enums.FieldStatus;

public record FieldCreateDto(
    Integer id,
    FieldStatus status
) {
}