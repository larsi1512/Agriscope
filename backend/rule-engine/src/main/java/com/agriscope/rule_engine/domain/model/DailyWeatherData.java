package com.agriscope.rule_engine.domain.model;

import com.agriscope.rule_engine.domain.enums.ForecastType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DailyWeatherData {
    private String userId;
    private String email;
    private String farmId;
    private ForecastType forecastType = ForecastType.DAILY;
    private LocalDateTime date;

    private Double temperature_2m_max;
    private Double temperature_2m_min;
    private Double showers_sum;
    private Double rain_sum;
    private Double snowfall_sum;
    private Double wind_speed_10m_max;
    private Double daylight_duration;
    private Double et0_fao_evapotranspiration;
}