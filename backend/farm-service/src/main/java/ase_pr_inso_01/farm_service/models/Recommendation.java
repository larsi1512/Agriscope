package ase_pr_inso_01.farm_service.models;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "recommendations")
@Getter
@Setter
public class Recommendation {
    @Id
    private String id;
    private String fieldId;
    private Date dateAdded;
    private String status;
}
