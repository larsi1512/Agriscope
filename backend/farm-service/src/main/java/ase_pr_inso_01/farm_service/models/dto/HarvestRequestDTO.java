package ase_pr_inso_01.farm_service.models.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
public class HarvestRequestDTO {
    private Date harvestDate;
    private List<FeedbackAnswerDTO> answers;
}