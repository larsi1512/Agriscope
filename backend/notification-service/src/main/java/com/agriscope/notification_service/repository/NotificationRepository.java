package com.agriscope.notification_service.repository;

import com.agriscope.notification_service.model.NotificationDocument;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    List<NotificationDocument> findByFarmIdOrderByCreatedAtDesc(String farmId);

    List<NotificationDocument> findByFarmIdAndReadFalseOrderByCreatedAtDesc(String farmId);

    @Aggregation(pipeline = {
            "{ '$match': { 'farmId': ?0 } }",
            "{ '$group': { '_id': '$recommendationType', 'count': { '$sum': 1 } } }"
    })
    List<StatResult> countByRecommendationType(String farmId);

    @Aggregation(pipeline = {
            "{ '$match': { 'farmId': ?0, 'recommendationType': { '$in': ['IRRIGATE_NOW', 'HEAT_ALERT', 'FROST_ALERT', 'STORM_ALERT','SAFETY_ALERT'] } } }",
            "{ '$group': { '_id': '$recommendedSeed', 'count': { '$sum': 1 } } }"
    })
    List<StatResult> countCriticalAlertsBySeed(String farmId);

    class StatResult {
        public String _id;
        public Long count;
    }
}
