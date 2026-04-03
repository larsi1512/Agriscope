package ase_pr_inso_01.farm_service.models;

import lombok.Data;

@Data
public class HarvestFeedbackAnswer {
    private String questionId;
    private String targetParameter;
    private String answerLabel;
    private Integer answerValue;
    private Double multiplier;
}