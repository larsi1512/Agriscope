package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.HarvestFeedbackAnswer;
import ase_pr_inso_01.farm_service.models.HarvestHistory;
import ase_pr_inso_01.farm_service.models.dto.FeedbackAnswerDTO;
import ase_pr_inso_01.farm_service.models.dto.FeedbackOptionDTO;
import ase_pr_inso_01.farm_service.models.dto.HarvestRequestDTO;
import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
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
public class FarmServiceImplHarvestFieldTests {

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
        harvestHistoryRepository.deleteAll();

    }


    @Test
    void ShouldHarvestFieldSuccessfully() {
        String farmId = "farm-harvest-123";
        Integer fieldId = 1;

        Field field = new Field();
        field.setId(fieldId);
        field.setStatus(FieldStatus.PLANTED);
        field.setSeedType(SeedType.CORN);
        field.setPlantedDate(new Date());

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        HarvestRequestDTO request = new HarvestRequestDTO();
        request.setHarvestDate(new Date());
        request.setAnswers(new ArrayList<>());

        // 2. Execute
        farmService.harvestField(farmId, fieldId, request);

        // 3. Verify Field State (Current State)
        Farm updatedFarm = farmRepository.findById(farmId).orElseThrow();
        Field updatedField = updatedFarm.getFields()[0];
        assertEquals(FieldStatus.EMPTY, updatedField.getStatus(), "Field should be reset to EMPTY");
        assertNull(updatedField.getSeedType(), "Field seed type should be cleared");

        // 4. Verify History State (Archived Data)
        List<HarvestHistory> history = harvestHistoryRepository.findAll();
        assertFalse(history.isEmpty(), "Harvest history should have been created");

        // Find the specific record for this farm to avoid "null" from other tests
        HarvestHistory savedHistory = history.stream()
                .filter(h -> farmId.equals(h.getFarmId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("History record not found for farmId: " + farmId));

        assertEquals(SeedType.CORN, savedHistory.getSeedType(), "History must record that CORN was harvested");
    }

    @Test
    void ShouldFailHarvestFieldNonExistentFarm() {
        String nonExistentFarmId = "missing-farm-id";
        HarvestRequestDTO request = new HarvestRequestDTO();
        request.setHarvestDate(new Date());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.harvestField(nonExistentFarmId, 1, request);
        });

        assertEquals("Farm not found", exception.getMessage());
    }

    @Test
    void ShouldFailHarvestFieldFieldNotFoundInFarm() {
        String farmId = "farm-with-no-fields";
        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[0]);
        farmRepository.save(farm);

        HarvestRequestDTO request = new HarvestRequestDTO();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.harvestField(farmId, 999, request); // ID 999 doesn't exist
        });

        assertEquals("Field not found", exception.getMessage());
    }

    @Test
    void ShouldFailHarvestFieldFieldIsAlreadyEmpty() {
        String farmId = "farm-123";
        Integer fieldId = 1;

        Field emptyField = new Field();
        emptyField.setId(fieldId);
        emptyField.setStatus(FieldStatus.EMPTY);

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[]{emptyField});
        farmRepository.save(farm);

        HarvestRequestDTO request = new HarvestRequestDTO();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.harvestField(farmId, fieldId, request);
        });

        assertEquals("Cannot harvest an empty field", exception.getMessage());
    }


    @Test
    void ShouldHarvestFieldsCorrectly() {

        harvestHistoryRepository.deleteAll();
        farmRepository.deleteAll();

        String farmId = "farm-mapping-test";
        Integer fieldId = 10;

        Field field = new Field();
        field.setId(fieldId);
        field.setStatus(FieldStatus.PLANTED);
        field.setSeedType(SeedType.CORN);
        field.setPlantedDate(new Date());

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        HarvestRequestDTO request = new HarvestRequestDTO();
        request.setHarvestDate(new Date());

        FeedbackAnswerDTO feedbackDTO = createValidFeedback("yield-q1", "High Yield", 1.5);
        request.setAnswers(List.of(feedbackDTO));

        farmService.harvestField(farmId, fieldId, request);

        List<HarvestHistory> historyList = harvestHistoryRepository.findAll();
        assertFalse(historyList.isEmpty(), "History should have been saved");

        HarvestHistory savedHistory = historyList.getFirst();

        assertNotNull(savedHistory.getFeedbackAnswers(), "Feedback list should not be null");
        assertFalse(savedHistory.getFeedbackAnswers().isEmpty(), "Feedback list should not be empty");

        HarvestFeedbackAnswer savedAnswer = savedHistory.getFeedbackAnswers().getFirst();

        assertEquals("yield-q1", savedAnswer.getQuestionId());
        assertEquals("High Yield", savedAnswer.getAnswerLabel());
        assertEquals(10, savedAnswer.getAnswerValue());
        assertEquals(1.5, savedAnswer.getMultiplier());
    }

    @Test
    void ShouldLogErrorMessage_WhenRabbitMQFails() {
        String farmId = "farm-rabbit-fail";
        Integer fieldId = 10;

        Field field = new Field();
        field.setId(fieldId);
        field.setStatus(FieldStatus.PLANTED);

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        HarvestRequestDTO request = new HarvestRequestDTO();
        request.setHarvestDate(new Date());

        doThrow(new RuntimeException("RabbitMQ Connection Refused"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Map.class));

        assertDoesNotThrow(() -> {
            farmService.harvestField(farmId, fieldId, request);
        });


        Farm updatedFarm = farmRepository.findById(farmId).orElseThrow();
        assertEquals(FieldStatus.EMPTY, updatedFarm.getFields()[0].getStatus());

        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("farm_events"), eq("field.harvested"), any(Map.class));
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
