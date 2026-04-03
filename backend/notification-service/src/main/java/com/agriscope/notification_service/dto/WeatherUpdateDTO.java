package com.agriscope.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherUpdateDTO {

    private String type;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("farm_id")
    private String farmId;

    private String email;

    private Double lat;
    private Double lon;

    private List<ForecastDTO> forecast;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastDTO {
        @JsonProperty("temperature_2m")
        private Double temperature;

        @JsonProperty("weather_code")
        private Integer weatherCode;

        private String time;
    }
}