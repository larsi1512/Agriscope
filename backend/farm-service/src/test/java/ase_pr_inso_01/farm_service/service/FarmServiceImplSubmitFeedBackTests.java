package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.HarvestFeedbackAnswer;
import ase_pr_inso_01.farm_service.models.HarvestHistory;
import ase_pr_inso_01.farm_service.models.dto.FeedbackAnswerDTO;
import ase_pr_inso_01.farm_service.models.dto.FeedbackOptionDTO;
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
public class FarmServiceImplSubmitFeedBackTests {

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
        when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(mockUser);

    }

    @Test
    void ShouldUpdateFeedbackSuccessfully() {
        // 1. Arrange: Create history with one "old" answer
        HarvestHistory history = new HarvestHistory();
        history.setId("hist-123");
        history.setFarmId("farm-1");

        HarvestFeedbackAnswer oldAnswer = new HarvestFeedbackAnswer();
        oldAnswer.setQuestionId("old-q");
        history.getFeedbackAnswers().add(oldAnswer);

        harvestHistoryRepository.save(history);

        FeedbackAnswerDTO newAnswer = createValidFeedback("new-q", "Better Yield", 1.2);

        newAnswer.setTargetParameter("yield_quality");

        farmService.submitFeedback("hist-123", List.of(newAnswer));

        HarvestHistory updated = harvestHistoryRepository.findById("hist-123").orElseThrow();

        assertEquals(1, updated.getFeedbackAnswers().size());
        assertEquals("new-q", updated.getFeedbackAnswers().get(0).getQuestionId());
        assertEquals("yield_quality", updated.getFeedbackAnswers().get(0).getTargetParameter());
    }

    @Test
    void ShouldFailHistoryIdDoesNotExist() {
        String invalidId = "non-existent-id";
        List<FeedbackAnswerDTO> answers = new ArrayList<>();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.submitFeedback(invalidId, answers);
        });

        assertEquals("Harvest history not found", exception.getMessage());
    }

    @Test
    void ShouldClearFeedbackListIsNull() {
        HarvestHistory history = new HarvestHistory();
        history.setId("hist-clear");
        history.getFeedbackAnswers().add(new HarvestFeedbackAnswer());
        harvestHistoryRepository.save(history);

        farmService.submitFeedback("hist-clear", null);

        HarvestHistory updated = harvestHistoryRepository.findById("hist-clear").orElseThrow();
        assertTrue(updated.getFeedbackAnswers().isEmpty(), "Feedback list should have been cleared");
    }

    private FeedbackAnswerDTO createValidFeedback(String qId, String label, Double mult) {
        FeedbackOptionDTO option = new FeedbackOptionDTO();
        option.setLabel(label);
        option.setValue(10);
        option.setMultiplier(mult);

        FeedbackAnswerDTO dto = new FeedbackAnswerDTO();
        dto.setQuestionId(qId);
        dto.setSelectedOption(option);
        return dto;
    }



}
