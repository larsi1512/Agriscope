package ase_pr_inso_01.farm_service.controller.dto.farm;

import ase_pr_inso_01.farm_service.models.enums.SoilType;
import jakarta.validation.constraints.*;

public record FarmCreateDto(
    @NotBlank(message = "Farm name must not be empty")
    @Size(min = 1, max = 20, message = "Farm name must be between 1 and 20 characters")
    String name,

    @NotNull(message = "Latitude must not be null")
    @DecimalMin(value = "46.3", message = "Latitude must be within Austria (min: 46.3)")
    @DecimalMax(value = "49.1", message = "Latitude must be within Austria (max: 49.1)")
    Float latitude,

    @NotNull(message = "Longitude must not be null")
    @DecimalMin(value = "9.5", message = "Longitude must be within Austria (min: 9.5)")
    @DecimalMax(value = "17.2", message = "Longitude must be within Austria (max: 17.2)")
    Float longitude,

    @NotNull(message = "Soil type must not be null")
    SoilType soilType,

    @NotNull(message = "Fields must not be null")
    FieldCreateDto[] fields
) {
}