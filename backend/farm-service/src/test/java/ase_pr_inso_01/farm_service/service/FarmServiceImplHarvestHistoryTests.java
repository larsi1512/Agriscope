package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.HarvestFeedbackAnswer;
import ase_pr_inso_01.farm_service.models.HarvestHistory;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.repository.HarvestHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@ActiveProfiles("test")
public class FarmServiceImplHarvestHistoryTests {


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
        harvestHistoryRepository.deleteAll();
        farmRepository.deleteAll();
        UserDetailsDto mockUser = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(mockUser);

    }
    @Test
    void ShouldReturnHarvestHistoryForSpecificFarm() {
        String farmId = "farm-alpha";
        String otherFarmId = "farm-beta";

        HarvestHistory h1 = new HarvestHistory();
        h1.setFarmId(farmId);

        HarvestHistory h2 = new HarvestHistory();
        h2.setFarmId(otherFarmId);

        harvestHistoryRepository.saveAll(List.of(h1, h2));

        List<HarvestHistory> results = farmService.getHarvestHistory(farmId);

        assertEquals(1, results.size());
        assertEquals(farmId, results.getFirst().getFarmId());
    }


    @Test
    void ShouldReturnEmptyMap_WhenNoHistoryExists() {
        Map<String, Double> results = farmService.calculateFeedbackFactors("empty-farm");

        assertTrue(results.isEmpty());
    }

    @Test
    void ShouldIgnoreAnswersWithNullTargetParameter() {
        String farmId = "farm-null-params";
        HarvestHistory hist = new HarvestHistory();
        hist.setFarmId(farmId);

        HarvestFeedbackAnswer ans = new HarvestFeedbackAnswer();
        ans.setTargetParameter(null); // This should be filtered out
        ans.setMultiplier(5.0);

        hist.setFeedbackAnswers(List.of(ans));
        harvestHistoryRepository.save(hist);

        Map<String, Double> factors = farmService.calculateFeedbackFactors(farmId);
        assertTrue(factors.isEmpty(), "Should ignore answers where targetParameter is null");
    }


}
