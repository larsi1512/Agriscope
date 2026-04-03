package ase_pr_inso_01.farm_service.models;

import ase_pr_inso_01.farm_service.models.enums.SoilType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "farms")
@Getter
@Setter
public class Farm {
    @Id
    private String id;
    private String name;
    private Float latitude;
    private Float longitude;
    private SoilType soilType;
    private Field[] fields;
    private Recommendation[] recommendations;
    private String userId;
}
