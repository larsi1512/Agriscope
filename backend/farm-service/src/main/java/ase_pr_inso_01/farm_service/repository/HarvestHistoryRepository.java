package ase_pr_inso_01.farm_service.repository;

import ase_pr_inso_01.farm_service.models.HarvestHistory;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface HarvestHistoryRepository extends MongoRepository<HarvestHistory, String> {
    List<HarvestHistory> findByFarmId(String farmId);

    @Aggregation(pipeline = {
            "{ '$match': { 'farmId': ?0 } }",

            "{ '$unwind': '$feedbackAnswers' }",

            "{ '$group': { '_id': '$feedbackAnswers.answerValue', 'count': { '$sum': 1 } } }"
    })
    List<StatResult> countRatingsByFarmId(String farmId);

    void deleteByFarmId(String farmId);

    class StatResult {
        public Integer _id;
        public Long count;
    }
}