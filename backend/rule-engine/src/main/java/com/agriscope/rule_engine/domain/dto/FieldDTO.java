package com.agriscope.rule_engine.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FieldDTO {
    @JsonProperty("field_id")
    private String field_id;

    @JsonProperty("seed_type")
    private String seed_type;

    @JsonProperty("growth_stage")
    private String growth_stage;
}