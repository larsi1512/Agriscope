package com.agriscope.rule_engine.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherMessageDTO {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("farm_id")
    private String farmId;

    private String soil_type;

    private String email;

    private List<String> crops;

    private String type;

    private List<FieldDTO> fields;

    private List<WeatherForecastDTO> forecast;
}