package com.agriscope.rule_engine.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherForecastDTO {

    @JsonAlias({"date", "time"})
    private String time;

    @JsonProperty("temperature_2m")
    private Double temperature2m;

    @JsonProperty("wind_speed_10m")
    private Double windSpeed10m;

    private Double rain;
    private Double precipitation;
    private Double showers;
    private Double snowfall;

    @JsonProperty("weather_code")
    private Double weatherCode;

    @JsonProperty("soil_moisture_0_to_1cm")
    private Double soilMoisture0to1cm;

    @JsonProperty("soil_moisture_1_to_3cm")
    private Double soilMoisture1to3cm;

    @JsonProperty("soil_moisture_3_to_9cm")
    private Double soilMoisture3to9cm;

    @JsonProperty("soil_moisture_9_to_27cm")
    private Double soilMoisture9to27cm;

    @JsonProperty("precipitation_probability")
    private Double precipitationProbability;

    @JsonProperty("relative_humidity_2m")
    private Double relativeHumidity2m;

    @JsonProperty("wind_speed_10m_max")
    private Double windSpeed10mMax;

    @JsonProperty("sunshine_duration")
    private Double sunshineDuration;

    @JsonProperty("temperature_2m_max")
    private Double temperature2mMax;

    @JsonProperty("temperature_2m_min")
    private Double temperature2mMin;

    @JsonProperty("showers_sum")
    private Double showersSum;

    @JsonProperty("rain_sum")
    private Double rainSum;

    @JsonProperty("snowfall_sum")
    private Double snowfallSum;

    @JsonProperty("daylight_duration")
    private Double daylightDuration;

    @JsonProperty("et0_fao_evapotranspiration")
    private Double et0FaoEvapotranspiration;
}