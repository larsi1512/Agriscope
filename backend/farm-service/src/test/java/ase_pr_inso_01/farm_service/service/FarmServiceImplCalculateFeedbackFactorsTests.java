package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.HarvestFeedbackAnswer;
import ase_pr_inso_01.farm_service.models.HarvestHistory;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.repository.HarvestHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FarmServiceImplCalculateFeedbackFactorsTests {

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmService farmService;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private HarvestHistoryRepository harvestHistoryRepository;
    private final String TEST_EMAIL = "farmer@test.com";
    private final String TEST_USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        farmRepository.deleteAll();
        UserDetailsDto mockUser = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        Mockito.when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(mockUser);

    }

    @Test
    void ShouldCalculateAverageMultipliersPerParameter() {
        String farmId = "farm-stats-123";
        harvestHistoryRepository.deleteAll();

        HarvestHistory hist1 = new HarvestHistory();
        hist1.setFarmId(farmId);
        HarvestFeedbackAnswer ans1 = new HarvestFeedbackAnswer();
        ans1.setTargetParameter("GROWTH");
        ans1.setMultiplier(1.2);
        hist1.setFeedbackAnswers(List.of(ans1));

        HarvestHistory hist2 = new HarvestHistory();
        hist2.setFarmId(farmId);

        HarvestFeedbackAnswer ans2 = new HarvestFeedbackAnswer();
        ans2.setTargetParameter("GROWTH");
        ans2.setMultiplier(1.8);

        HarvestFeedbackAnswer ans3 = new HarvestFeedbackAnswer();
        ans3.setTargetParameter("YIELD");
        ans3.setMultiplier(2.0);

        hist2.setFeedbackAnswers(List.of(ans2, ans3));

        harvestHistoryRepository.saveAll(List.of(hist1, hist2));

        Map<String, Double> factors = farmService.calculateFeedbackFactors(farmId);

        assertEquals(2, factors.size());
        assertEquals(1.5, factors.get("GROWTH"), 0.001);
        assertEquals(2.0, factors.get("YIELD"), 0.001);
    }

}
