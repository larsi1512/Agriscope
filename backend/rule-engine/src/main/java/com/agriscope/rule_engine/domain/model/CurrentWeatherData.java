package com.agriscope.rule_engine.domain.model;

import com.agriscope.rule_engine.domain.enums.ForecastType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CurrentWeatherData {
    private String userId;
    private String email;
    private String farmId;
    private ForecastType forecastType = ForecastType.CURRENT;
    private LocalDateTime time;

    private Double temperature_2m;
    private Double wind_speed_10m;
    private Double rain;
    private Double precipitation;
    private Double showers;
    private Double snowfall;
    private Double weather_code;
}