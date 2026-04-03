package ase_pr_inso_01.farm_service.models;

import ase_pr_inso_01.farm_service.models.enums.SeedType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "harvest_history")
public class HarvestHistory {

    @Id
    private String id;

    private String farmId;
    private Integer originalFieldId;

    private SeedType seedType;
    private Date plantedDate;
    private Date harvestDate;

    private List<HarvestFeedbackAnswer> feedbackAnswers = new ArrayList<>();
}