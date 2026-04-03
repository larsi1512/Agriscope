package ase_pr_inso_01.farm_service.models;

import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.models.enums.GrowthStage;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;


@Document(collection = "fields")
@Getter
@Setter
public class Field {
    @Id
    private Integer id;
    private FieldStatus status;
    private SeedType seedType;
    private GrowthStage growthStage;
    private Date plantedDate;
    private Date harvestDate;
}
