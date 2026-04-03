package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.Farm;
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
public class FarmServiceImplDeleteHarvestHistoryTests {

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
    void ShouldFailDeleteHistory_WhenUserDoesNotOwnFarm() throws Exception {
        String historyId = "hist-123";
        String farmId = "stranger-farm-456";
        String actualOwnerId = "user-owner";
        String hackerUserId = "user-hacker";
        String hackerEmail = "hacker@example.com";

        HarvestHistory history = new HarvestHistory();
        history.setId(historyId);
        history.setFarmId(farmId);
        harvestHistoryRepository.save(history);

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setUserId(actualOwnerId);
        farmRepository.save(farm);

        UserDetailsDto hackerDto = new UserDetailsDto(hackerUserId, hackerEmail);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(hackerDto);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.deleteHarvestHistory(historyId, hackerEmail);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"),
                "Expected unauthorized message but got: " + exception.getMessage());

        HarvestHistory notUpdated = harvestHistoryRepository.findById(historyId).orElseThrow();
        assertNotNull(notUpdated, "History should still exist");
    }
    @Test
    void ShouldDeleteHistorySuccessfully() throws Exception {
        String historyId = "hist-789";
        String farmId = "my-farm-101";
        String userId = "legit-owner";
        String email = "owner@example.com";

        HarvestHistory history = new HarvestHistory();
        history.setId(historyId);
        history.setFarmId(farmId);
        history.getFeedbackAnswers().add(new HarvestFeedbackAnswer()); // Add dummy feedback
        harvestHistoryRepository.save(history);

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setUserId(userId);
        farmRepository.save(farm);

        UserDetailsDto ownerDto = new UserDetailsDto(userId, email);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(ownerDto);

        farmService.deleteHarvestHistory(historyId, email);

        HarvestHistory updated = harvestHistoryRepository.findById(historyId).orElseThrow();
        assertTrue(updated.getFeedbackAnswers().isEmpty(), "Feedback should have been cleared");
    }

    @Test
    void ShouldDeleteAllHistorySuccessfully() throws Exception {
        String farmId = "my-farm-99";
        String userId = "owner-123";
        String email = "owner@example.com";

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setUserId(userId);
        farmRepository.save(farm);

        HarvestHistory h1 = new HarvestHistory();
        h1.setFarmId(farmId);
        HarvestHistory h2 = new HarvestHistory();
        h2.setFarmId(farmId);

        HarvestHistory h3 = new HarvestHistory();
        h3.setFarmId("other-farm");

        harvestHistoryRepository.saveAll(List.of(h1, h2, h3));

        UserDetailsDto ownerDto = new UserDetailsDto(userId, email);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(ownerDto);

        farmService.deleteAllHarvestHistory(farmId, email);
        List<HarvestHistory> remainingHistory = harvestHistoryRepository.findAll();

        assertEquals(1, remainingHistory.size());
        assertEquals("other-farm", remainingHistory.get(0).getFarmId());

        assertTrue(harvestHistoryRepository.findByFarmId(farmId).isEmpty());
    }

    @Test
    void ShouldFailDeleteAllHistory_WhenUserDoesNotOwnFarm() throws Exception {

        String farmId = "private-farm";
        String actualOwnerId = "real-owner";
        String hackerUserId = "hacker-id";
        String hackerEmail = "hacker@example.com";

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setUserId(actualOwnerId);
        farmRepository.save(farm);

        HarvestHistory h1 = new HarvestHistory();
        h1.setFarmId(farmId);
        harvestHistoryRepository.save(h1);

        UserDetailsDto hackerDto = new UserDetailsDto(hackerUserId, hackerEmail);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(hackerDto);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.deleteAllHarvestHistory(farmId, hackerEmail);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));

        assertFalse(harvestHistoryRepository.findByFarmId(farmId).isEmpty(),
                "History should still exist because the user was unauthorized");
    }

}
