package ase_pr_inso_01.farm_service.controller.dto.farm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for checking if user has farms
 * Used during login to determine routing
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FarmCheckDto {
    private boolean hasFarms;
    private int farmCount;
}