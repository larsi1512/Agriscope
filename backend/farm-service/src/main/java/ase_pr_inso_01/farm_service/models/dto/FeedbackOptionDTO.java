package ase_pr_inso_01.farm_service.models.dto;

import lombok.Data;

@Data
public class FeedbackOptionDTO {
    private String label;
    private Integer value;
    private Double multiplier;
}