package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.Seed;
import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.models.enums.GrowthStage;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.repository.SeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthStageScheduler {

    private final FarmRepository farmRepository;
    private final SeedRepository seedRepository;

//    @Scheduled(cron = "0 0 0 * * ?")
    @Scheduled(fixedRate = 30000) // test
    public void updateGrowthStages() {
        log.info("Starting daily growth stage update...");

        Map<String, Seed> seedMap = seedRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getSeedType().name(), Function.identity()));

        log.info("Loaded {} seeds from DB: {}", seedMap.size(), seedMap.keySet());
        List<Farm> farms = farmRepository.findAll();
        int updatedCount = 0;

        for (Farm farm : farms) {
            boolean farmChanged = false;

            if (farm.getFields() == null) continue;

            for (Field field : farm.getFields()) {
                if (field.getStatus() != FieldStatus.EMPTY) {
                    log.info("Checking Field {} (Farm {}): Status={}, Seed={}, Planted={}",
                            field.getId(), farm.getId(), field.getStatus(), field.getSeedType(), field.getPlantedDate());
                }
                if (field.getStatus() == FieldStatus.PLANTED && field.getPlantedDate() != null) {

                    Seed seedRule = seedMap.get(field.getSeedType().name());
                    if (seedRule == null) {
                        log.warn("CRITICAL: No seed rule found for type '{}'. Available: {}",
                                field.getSeedType(), seedMap.keySet());
                        continue;
                    }

                    long timeElapsed = calculateDaysElapsed(field.getPlantedDate());

                    GrowthStage newStage = determineStage(timeElapsed, seedRule);

                    if (newStage != field.getGrowthStage()) {
                        log.info("UPDATING Field {} ({}): {} -> {}",
                                field.getId(), field.getSeedType(), field.getGrowthStage(), newStage);

                        field.setGrowthStage(newStage);

                        if (newStage == GrowthStage.READY) {
                            field.setStatus(FieldStatus.READY);
                        }

                        farmChanged = true;
                        updatedCount++;
                    } else {
                        log.info("No stage change needed yet (Current: {}, Required for next: ?)", field.getGrowthStage());
                    }
                }
            }

            if (farmChanged) {
                farmRepository.save(farm);
            }
        }

        log.info("Growth stage update completed. Updated {} fields.", updatedCount);
    }

    private long calculateDaysElapsed(Date plantedDate) {
        LocalDate planted = plantedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();
        return ChronoUnit.DAYS.between(planted, now);
    }

    private long calculateMinutesElapsed(Date plantedDate) {
        LocalDateTime planted = plantedDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDateTime now = LocalDateTime.now();

        return Math.max(0, ChronoUnit.MINUTES.between(planted, now));
    }

    private GrowthStage determineStage(long days, Seed seed) {
        if (days >= seed.getDaysToReady()) {
            return GrowthStage.READY;
        } else if (days >= seed.getDaysToMature()) {
            return GrowthStage.MATURE;
        } else if (days >= seed.getDaysToYoung()) {
            return GrowthStage.YOUNG;
        } else {
            return GrowthStage.SEEDLING;
        }
    }
}