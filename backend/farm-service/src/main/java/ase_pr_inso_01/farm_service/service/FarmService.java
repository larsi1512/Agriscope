package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.FarmCheckDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.FarmCreateDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.FarmDetailsDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.FieldDetailsDto;
import ase_pr_inso_01.farm_service.models.HarvestHistory;
import ase_pr_inso_01.farm_service.models.dto.FeedbackAnswerDTO;
import ase_pr_inso_01.farm_service.models.dto.HarvestRequestDTO;

import java.util.List;
import java.util.Map;

//TODO: Add comments
public interface FarmService {
    FarmDetailsDto createFarm(FarmCreateDto dto, String email) throws Exception;

    FarmCheckDto checkUserHasFarms(String email) throws Exception;

    List<FarmDetailsDto> getFarmsByUserEmail(String email) throws Exception;

    FarmDetailsDto getFarmById(String farmId, String email) throws Exception;

    FarmDetailsDto updateField(String farmId, FieldDetailsDto fieldUpdate, String email) throws Exception;

    void deleteFarm(String farmId, String email) throws Exception;

    void harvestField(String farmId, Integer fieldId, HarvestRequestDTO request);

    void submitFeedback(String historyId, List<FeedbackAnswerDTO> answers);

    List<HarvestHistory> getHarvestHistory(String farmId);

    public Map<String, Double> calculateFeedbackFactors(String farmId);

    void deleteHarvestHistory(String historyId, String email) throws Exception;

    void deleteAllHarvestHistory(String farmId, String email) throws Exception;
}
