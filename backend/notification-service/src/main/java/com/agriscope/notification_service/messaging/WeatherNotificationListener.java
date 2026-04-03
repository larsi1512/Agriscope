package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.dto.WeatherUpdateDTO;
import com.agriscope.notification_service.model.WeatherDocument;
import com.agriscope.notification_service.repository.WeatherRepository;
import com.agriscope.notification_service.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static com.agriscope.notification_service.config.RabbitMQConfig.NOTIFICATION_WEATHER_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherNotificationListener {

    private final WebSocketService webSocketService;
    private final WeatherRepository weatherRepository;

    @RabbitListener(queues = NOTIFICATION_WEATHER_QUEUE)
    public void handleWeather(WeatherUpdateDTO weatherData) {

        if (!"current".equalsIgnoreCase(weatherData.getType())) {
            return;
        }

        if (weatherData.getUserId() == null) {
            log.warn("Weather payload without user_id: {}", weatherData);
            return;
        }

        if (weatherData.getForecast() != null && !weatherData.getForecast().isEmpty()) {
            WeatherUpdateDTO.ForecastDTO currentForecast = weatherData.getForecast().getFirst();

            var simplifiedPayload = new LinkedHashMap<>();
            simplifiedPayload.put("user_id", weatherData.getUserId());
            simplifiedPayload.put("time", currentForecast.getTime());
            simplifiedPayload.put("farm_id", weatherData.getFarmId());
            simplifiedPayload.put("lat", weatherData.getLat());
            simplifiedPayload.put("lon", weatherData.getLon());
            simplifiedPayload.put("weather_code", currentForecast.getWeatherCode());
            simplifiedPayload.put("temp", currentForecast.getTemperature());

            webSocketService.sendWeatherToUser(weatherData.getFarmId(), simplifiedPayload);

            try {
                WeatherDocument doc = weatherRepository.findByFarmId(weatherData.getFarmId())
                        .orElse(new WeatherDocument());

                doc.setFarmId(weatherData.getFarmId());
                doc.setUserId(weatherData.getUserId());
                doc.setTemp(currentForecast.getTemperature());
                doc.setWeatherCode(currentForecast.getWeatherCode());

                doc.setTimestamp(parseDateTime(currentForecast.getTime()));

                doc.setStoredAt(LocalDateTime.now());

                weatherRepository.save(doc);
                log.info("Weather cached in DB for farm: {}", weatherData.getFarmId());
            } catch (Exception e) {
                log.error("Failed to cache weather for farm {}: {}", weatherData.getFarmId(), e.getMessage());
            }
        } else {
            log.warn("Received weather payload without forecast: {}", weatherData);
        }

        log.info("Sent weather update for user {}", weatherData.getUserId());
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr == null) return LocalDateTime.now();

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