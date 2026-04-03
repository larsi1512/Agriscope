package ase_pr_inso_01.farm_service.service.impl;

import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.mapper.FarmMapper;
import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.HarvestFeedbackAnswer;
import ase_pr_inso_01.farm_service.models.HarvestHistory;
import ase_pr_inso_01.farm_service.models.dto.FeedbackAnswerDTO;
import ase_pr_inso_01.farm_service.models.dto.HarvestRequestDTO;
import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.repository.HarvestHistoryRepository;
import ase_pr_inso_01.farm_service.service.FarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final HarvestHistoryRepository harvestHistoryRepository;

    public FarmServiceImpl(FarmRepository farmRepository, FarmMapper farmMapper, RestTemplate restTemplate, RabbitTemplate rabbitTemplate, HarvestHistoryRepository harvestHistoryRepository) {
        this.farmRepository = farmRepository;
        this.farmMapper = farmMapper;
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.harvestHistoryRepository = harvestHistoryRepository;
    }

    @Override
    public FarmDetailsDto createFarm(FarmCreateDto farm, String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);

        if (farmRepository.existsByNameAndUserId(farm.name(), user.getId())) {
            throw new RuntimeException("You already have a farm with this name");
        }

        Farm createdFarm = farmMapper.farmCreateDtoToFarm(farm, user.getId());
        farmRepository.save(createdFarm);

        Map<String, Double> factors = this.calculateFeedbackFactors(createdFarm.getId());
        FarmDetailsDto farmDto = farmMapper.farmToFarmDetailsDto(createdFarm, factors);
        sendFarmEvent(user.getId(), email, farmDto, "farm.created");

        return farmMapper.farmToFarmDetailsDto(createdFarm, factors);
    }

    /**
     * ⭐ Check if user has any farms
     */
    @Override
    public FarmCheckDto checkUserHasFarms(String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);
        List<Farm> farms = farmRepository.findByUserId(user.getId());

        return new FarmCheckDto(
                !farms.isEmpty(),  // hasFarms
                farms.size()       // farmCount
        );
    }

    @Override
    public List<FarmDetailsDto> getFarmsByUserEmail(String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);
        return this.getFarmsByUserId(user.getId());
    }

    @Override
    public FarmDetailsDto updateField(String farmId, FieldDetailsDto fieldUpdate, String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);

        // Fetch the farm by ID
        Optional<Farm> optionalFarm = farmRepository.findById(farmId);

        if (optionalFarm.isEmpty()) {
            throw new RuntimeException("Farm not found with ID: " + farmId);
        }

        Farm farm = optionalFarm.get();

        // ⭐ SECURITY: Check if farm belongs to user
        if (!farm.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This farm does not belong to you");
        }


        // Find the field to update
        Field fieldToUpdate = Arrays.stream(farm.getFields())
                .filter(f -> f.getId().equals(fieldUpdate.id()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Field not found with ID: " + fieldUpdate.id()));

        // Update the found field
        fieldToUpdate.setStatus(fieldUpdate.status());
        fieldToUpdate.setSeedType(fieldUpdate.seedType());
        fieldToUpdate.setPlantedDate(fieldUpdate.plantedDate());
        fieldToUpdate.setGrowthStage(fieldUpdate.growthStage());

        // Save the changes
        Farm updatedFarm = farmRepository.save(farm);
        Map<String, Double> factors = this.calculateFeedbackFactors(farmId);
        FarmDetailsDto farmDto = farmMapper.farmToFarmDetailsDto(updatedFarm, factors);
        sendFarmEvent(user.getId(), email, farmDto, "farm.updated");
        return farmMapper.farmToFarmDetailsDto(updatedFarm, factors);
    }

    @Override
    public FarmDetailsDto getFarmById(String farmId, String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);

        Optional<Farm> optionalFarm = farmRepository.findById(farmId);
        if (optionalFarm.isEmpty()) {
            throw new RuntimeException("Farm not found with ID: " + farmId);
        }
        Farm farm = optionalFarm.get();

        if (!farm.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This farm does not belong to you");
        }

        Map<String, Double> factors = this.calculateFeedbackFactors(farmId);

        return farmMapper.farmToFarmDetailsDto(farm, factors);
    }

    @Override
    public void deleteFarm(String farmId, String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);

        Optional<Farm> optionalFarm = farmRepository.findById(farmId);

        if (optionalFarm.isEmpty()) {
            throw new RuntimeException("Farm not found with ID: " + farmId);
        }

        Farm farm = optionalFarm.get();

        // Check if farm belongs to user
        if (!farm.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This farm does not belong to you");
        }

        farmRepository.delete(farm);
    }

    private UserDetailsDto getUserDetails(String email) throws Exception {
        String baseUrl = System.getenv("API_GATEWAY_URL");
        if (baseUrl == null) {
            baseUrl = "http://localhost:8080";
        }
        String url = baseUrl + "/api/users/by-email/" + email;

        try {
            UserDetailsDto userDto = restTemplate.getForObject(url, UserDetailsDto.class);

            if (userDto == null) {
                throw new Exception("User not found: " + email);
            }

            return userDto;
        } catch (Exception e) {
            throw new Exception("User not found: " + email);
        }
    }

    private List<FarmDetailsDto> getFarmsByUserId(String userId) {
        return farmRepository.findByUserId(userId).stream()
                .map(farm -> {
                    Map<String, Double> factors = this.calculateFeedbackFactors(farm.getId());
                    return farmMapper.farmToFarmDetailsDto(farm, factors);
                })
                .toList();
    }

    private void sendFarmEvent(String userId, String email, FarmDetailsDto farmDto, String routingKey) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("user_id", userId);
            message.put("email", email);
            message.put("farm", farmDto);

            rabbitTemplate.convertAndSend(
                    "farm_events",
                    routingKey,
                    message
            );

            System.out.println("Sent RabbitMQ event: " + routingKey + " for farm: " + farmDto.id());
        } catch (Exception e) {
            System.err.println("Failed to send RabbitMQ event (" + routingKey + "): " + e.getMessage());
        }
    }

    public void harvestField(String farmId, Integer fieldId, HarvestRequestDTO request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        Field field = Arrays.stream(farm.getFields())
                .filter(f -> f.getId().equals(fieldId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Field not found"));

        if (field.getStatus() == FieldStatus.EMPTY) {
            throw new RuntimeException("Cannot harvest an empty field");
        }

        HarvestHistory history = new HarvestHistory();
        history.setFarmId(farmId);
        history.setOriginalFieldId(fieldId);
        history.setSeedType(field.getSeedType());
        history.setPlantedDate(field.getPlantedDate());
        history.setHarvestDate(request.getHarvestDate());

        if (request.getAnswers() != null) {
            for (FeedbackAnswerDTO dto : request.getAnswers()) {
                HarvestFeedbackAnswer answer = new HarvestFeedbackAnswer();
                answer.setQuestionId(dto.getQuestionId());
                answer.setAnswerLabel(dto.getSelectedOption().getLabel());
                answer.setAnswerValue(dto.getSelectedOption().getValue());
                answer.setMultiplier(dto.getSelectedOption().getMultiplier());

                history.getFeedbackAnswers().add(answer);
            }
        }

        harvestHistoryRepository.save(history);

        field.setStatus(FieldStatus.EMPTY);
        field.setSeedType(null);
        field.setPlantedDate(null);
        field.setGrowthStage(null);

        farmRepository.save(farm);
        sendHarvestEvent(farmId, fieldId);
    }

    private void sendHarvestEvent(String farmId, Integer fieldId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("farmId", farmId);
            message.put("fieldId", String.valueOf(fieldId));
            message.put("event", "field.harvested");

            rabbitTemplate.convertAndSend(
                    "farm_events",
                    "field.harvested",
                    message
            );
            System.out.println("Sent Harvest Event for field: " + fieldId);
        } catch (Exception e) {
            System.err.println("Failed to send Harvest event: " + e.getMessage());
        }
    }

    public void submitFeedback(String historyId, List<FeedbackAnswerDTO> answers) {
        HarvestHistory history = harvestHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Harvest history not found"));

        history.getFeedbackAnswers().clear();

        if (answers != null) {
            for (FeedbackAnswerDTO dto : answers) {
                HarvestFeedbackAnswer answer = new HarvestFeedbackAnswer();
                answer.setQuestionId(dto.getQuestionId());
                answer.setTargetParameter(dto.getTargetParameter());
                answer.setAnswerLabel(dto.getSelectedOption().getLabel());
                answer.setAnswerValue(dto.getSelectedOption().getValue());
                answer.setMultiplier(dto.getSelectedOption().getMultiplier());

                history.getFeedbackAnswers().add(answer);
            }
        }

        harvestHistoryRepository.save(history);
    }


    public List<HarvestHistory> getHarvestHistory(String farmId) {
        return harvestHistoryRepository.findByFarmId(farmId);
    }

    public Map<String, Double> calculateFeedbackFactors(String farmId) {
        List<HarvestHistory> history = harvestHistoryRepository.findByFarmId(farmId);
        Map<String, Double> factors = new HashMap<>();

        if (history.isEmpty()) return factors;

        Map<String, List<Double>> groupedMultipliers = history.stream()
                .flatMap(h -> h.getFeedbackAnswers().stream())
                .filter(a -> a.getTargetParameter() != null)
                .collect(Collectors.groupingBy(
                        HarvestFeedbackAnswer::getTargetParameter,
                        Collectors.mapping(HarvestFeedbackAnswer::getMultiplier, Collectors.toList())
                ));

        groupedMultipliers.forEach((param, values) -> {
            double avg = values.stream().mapToDouble(d -> d).average().orElse(1.0);
            factors.put(param, avg);
        });

        return factors;
    }

    @Override
    public void deleteHarvestHistory(String historyId, String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);

        HarvestHistory history = harvestHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Harvest history not found with ID: " + historyId));

        Farm farm = farmRepository.findById(history.getFarmId())
                .orElseThrow(() -> new RuntimeException("Associated farm not found"));

        if (!farm.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You cannot delete history for a farm you do not own");
        }

        history.getFeedbackAnswers().clear();
        harvestHistoryRepository.save(history);
    }

    @Override
    public void deleteAllHarvestHistory(String farmId, String email) throws Exception {
        UserDetailsDto user = this.getUserDetails(email);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found with ID: " + farmId));

        if (!farm.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You cannot delete history for a farm you do not own");
        }

        
        harvestHistoryRepository.deleteByFarmId(farmId);
    }
}