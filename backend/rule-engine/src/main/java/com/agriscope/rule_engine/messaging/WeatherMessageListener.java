package com.agriscope.rule_engine.messaging;

import com.agriscope.rule_engine.config.RabbitMQConfig;
import com.agriscope.rule_engine.domain.dto.FieldDTO;
import com.agriscope.rule_engine.domain.dto.WeatherForecastDTO;
import com.agriscope.rule_engine.domain.dto.WeatherMessageDTO;
import com.agriscope.rule_engine.domain.enums.ForecastType;
import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.enums.SoilType;
import com.agriscope.rule_engine.domain.model.CurrentWeatherData;
import com.agriscope.rule_engine.domain.model.FarmDetails;
import com.agriscope.rule_engine.domain.model.HourlyWeatherData;
import com.agriscope.rule_engine.service.RuleEvaluationService;
import com.agriscope.rule_engine.domain.model.DailyWeatherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeatherMessageListener {

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${farm.service.url:http://api-gateway:8080/api/farms}")
    private String farmServiceUrl;

    @RabbitListener(
            queues = RabbitMQConfig.WEATHER_QUEUE,
            concurrency = "1-4")
    public void handleMessage(WeatherMessageDTO message) {
        if (message == null) {
            log.warn("Received null message, skipping processing.");
            return;
        }

        try {
            String userId = message.getUserId();
            String email = message.getEmail();
            String farmId = message.getFarmId();
            String type = message.getType();
            String soilType = message.getSoil_type();
            List<WeatherForecastDTO> forecast = message.getForecast();
            List<FieldDTO> fields = message.getFields();

            if (type == null || forecast == null || forecast.isEmpty()) {
                log.warn("Received invalid weather message for userId={}, farmId={}", userId, farmId);
                return;
            }
            processForecastByType(forecast, type, userId, email, farmId, fields, soilType);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
    }


    private void processForecastByType(List<WeatherForecastDTO> forecastData,
                                       String forecastType,
                                       String userId,
                                       String email,
                                       String farmId,
                                       List<FieldDTO> fields,
                                       String soilType) {
        switch (forecastType.toUpperCase()) {
            case "CURRENT":
                processCurrentForecast(forecastData, userId, email, farmId, fields);
                break;
            case "HOURLY":
                processHourlyForecast(forecastData, userId, email, farmId, fields, soilType);
                break;
            case "DAILY":
                processDailyForecast(forecastData, userId, email, farmId, fields);
                break;
            default:
                log.warn("Unknown forecast type: {}", forecastType);
        }
    }

    private void processCurrentForecast(List<WeatherForecastDTO> forecastData,
                                        String userId,
                                        String email,
                                        String farmId,
                                        List<FieldDTO> fields) {
        if (forecastData.isEmpty()) {
            log.warn("Empty CURRENT forecast list for user {}, farm {}", userId, farmId);
            return;
        }

        WeatherForecastDTO dto = forecastData.getFirst();
        CurrentWeatherData weatherData = convertToCurrentWeatherData(dto, userId, email, farmId);
        weatherData.setForecastType(ForecastType.CURRENT);

        log.info("Current - user={}, farm={}, Temp: {}C, Rain: {}mm, Wind: {}m/s",
                userId,
                farmId,
                weatherData.getTemperature_2m(),
                weatherData.getRain(),
                weatherData.getWind_speed_10m());
        Map<String, Double> feedbackFactors = fetchFeedbackFactors(farmId);

        ruleEvaluationService.evaluateCurrentDataForFarm(weatherData, fields, feedbackFactors);
    }

    private void processHourlyForecast(List<WeatherForecastDTO> forecastData,
                                       String userId,
                                       String email,
                                       String farmId,
                                       List<FieldDTO> fields,
                                       String soilType) {
        List<HourlyWeatherData> hourlyList = new ArrayList<>();

        for (WeatherForecastDTO dto : forecastData) {
            hourlyList.add(convertToHourlyWeatherData(dto, userId, email, farmId));
        }
        Map<String, Double> feedbackFactors = fetchFeedbackFactors(farmId);
        FarmDetails farm = new FarmDetails();
        farm.setFarmId(farmId);
        farm.setSoilType(soilType != null ? SoilType.valueOf(soilType) : null);
        farm.setFeedbackFactors(feedbackFactors);

        log.info("Processing Irrigation logic for {} hours of data", hourlyList.size());
        ruleEvaluationService.evaluateHourlDataForFarm(hourlyList, farm, fields);
    }

    private void processDailyForecast(List<WeatherForecastDTO> forecastData,
                                      String userId,
                                      String email,
                                      String farmId,
                                      List<FieldDTO> fields) {

        List<DailyWeatherData> dailyList = new ArrayList<>();

        for (WeatherForecastDTO dto : forecastData) {
            DailyWeatherData daily = new DailyWeatherData();
            daily.setUserId(userId);
            daily.setEmail(email);
            daily.setFarmId(farmId);
            daily.setForecastType(ForecastType.DAILY);
            daily.setTemperature_2m_max(dto.getTemperature2mMax());
            daily.setTemperature_2m_min(dto.getTemperature2mMin());
            daily.setRain_sum(dto.getRainSum());
            daily.setWind_speed_10m_max(dto.getWindSpeed10mMax());
            daily.setEt0_fao_evapotranspiration(dto.getEt0FaoEvapotranspiration());

            if (dto.getTime() != null) {
                daily.setDate(parseDateTime(dto.getTime()));
            } else {
                log.warn("Missing date in forecast DTO, defaulting to NOW");
                daily.setDate(LocalDateTime.now());
            }
            dailyList.add(daily);
        }
        Map<String, Double> feedbackFactors = fetchFeedbackFactors(farmId);

        log.info("Processing DAILY rules for {} days forecast", dailyList.size());
        ruleEvaluationService.evaluateDailyRules(dailyList, fields, feedbackFactors);
    }

    private Map<String, Double> fetchFeedbackFactors(String farmId) {
        try {
            String url = farmServiceUrl + "/" + farmId + "/feedback-factors";

            ResponseEntity<Map<String, Double>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Double>>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Could not fetch feedback factors for farm {}: {}", farmId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private HourlyWeatherData convertToHourlyWeatherData(WeatherForecastDTO dto, String userId, String email, String farmId) {
        HourlyWeatherData data = new HourlyWeatherData();
        data.setUserId(userId);
        data.setEmail(email);
        data.setFarmId(farmId);
        data.setForecastType(ForecastType.HOURLY);

        data.setTemperature_2m(dto.getTemperature2m());
        data.setRain(dto.getRain());
        data.setPrecipitation(dto.getPrecipitation());
        data.setPrecipitation_probability(dto.getPrecipitationProbability());
        data.setWind_speed_10m_max(dto.getWindSpeed10m());

        data.setSoil_moisture_0_to_1cm(dto.getSoilMoisture0to1cm());
        data.setSoil_moisture_1_to_3cm(dto.getSoilMoisture1to3cm());
        data.setSoil_moisture_3_to_9cm(dto.getSoilMoisture3to9cm());
        data.setSoil_moisture_9_to_27cm(dto.getSoilMoisture9to27cm());

        data.setEt0_fao_evapotranspiration(dto.getEt0FaoEvapotranspiration());

        if (dto.getTime() != null) {
            data.setDate(parseDateTime(dto.getTime()));
        }

        return data;
    }

    private CurrentWeatherData convertToCurrentWeatherData(WeatherForecastDTO dto,
                                                           String userId,
                                                           String email,
                                                           String farmId) {
        CurrentWeatherData weatherData = new CurrentWeatherData();
        weatherData.setUserId(userId);
        weatherData.setEmail(email);
        weatherData.setFarmId(farmId);

        weatherData.setTemperature_2m(dto.getTemperature2m());
        weatherData.setWind_speed_10m(dto.getWindSpeed10m());
        weatherData.setRain(dto.getRain());
        weatherData.setPrecipitation(dto.getPrecipitation());
        weatherData.setShowers(dto.getShowers());
        weatherData.setSnowfall(dto.getSnowfall());
        weatherData.setWeather_code(dto.getWeatherCode());

        if (dto.getTime() != null) {
            weatherData.setTime(parseDateTime(dto.getTime()));
        } else {
            weatherData.setTime(LocalDateTime.now());
        }

        return weatherData;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            String normalized = dateTimeStr.replace(" ", "T");
            if (normalized.contains("+")) {
                normalized = normalized.substring(0, normalized.indexOf("+"));
            }
            return LocalDateTime.parse(normalized);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}, using current time", dateTimeStr);
            return LocalDateTime.now();
        }
    }
}
