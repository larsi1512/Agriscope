package com.agriscope.notification_service.repository;

import com.agriscope.notification_service.model.WeatherDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface WeatherRepository extends MongoRepository<WeatherDocument, String> {
    Optional<WeatherDocument> findByFarmId(String farmId);
}