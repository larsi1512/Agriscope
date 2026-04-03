package com.agriscope.notification_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "weather_cache")
public class WeatherDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String farmId;

    private String userId;

    private Double temp;
    private Double rain;
    private Double wind;
    private Integer weatherCode;

    private LocalDateTime timestamp;

    @Indexed(expireAfter = "900s")
    private LocalDateTime storedAt;
}