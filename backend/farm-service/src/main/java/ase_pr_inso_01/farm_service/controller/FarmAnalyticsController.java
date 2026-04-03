package ase_pr_inso_01.farm_service.controller;

import ase_pr_inso_01.farm_service.repository.HarvestHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/farm-analytics")
@RequiredArgsConstructor
public class FarmAnalyticsController {

    private final HarvestHistoryRepository feedbackRepo;

    @GetMapping("/feedback-stats/{farmId}")
    public ResponseEntity<Map<Integer, Long>> getFeedbackStats(@PathVariable String farmId) {
        List<HarvestHistoryRepository.StatResult> results = feedbackRepo.countRatingsByFarmId(farmId);

        Map<Integer, Long> histogram = results.stream()
                .collect(Collectors.toMap(res -> res._id, res -> res.count));

        return ResponseEntity.ok(histogram);
    }
}