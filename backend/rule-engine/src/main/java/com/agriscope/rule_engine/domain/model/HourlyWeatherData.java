package com.agriscope.rule_engine.domain.model;

import com.agriscope.rule_engine.domain.enums.ForecastType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HourlyWeatherData {
    private String userId;
    private String email;
    private String farmId;
    private ForecastType forecastType = ForecastType.HOURLY;
    private LocalDateTime date;

    private Double temperature_2m;
    private Double soil_moisture_0_to_1cm;
    private Double soil_moisture_1_to_3cm;
    private Double soil_moisture_3_to_9cm;
    private Double soil_moisture_9_to_27cm;
    private Double rain;
    private Double snowfall;
    private Double precipitation;
    private Double daylight_duration;
    private Double precipitation_probability;
    private Double relative_humidity_2m;
    private Double wind_speed_10m_max;
    private Double sunshine_duration;
    private Double et0_fao_evapotranspiration;
}