package com.agriscope.rule_engine.service;

import com.agriscope.rule_engine.domain.dto.DailyAnalysis;
import com.agriscope.rule_engine.domain.dto.FieldDTO;
import com.agriscope.rule_engine.domain.enums.GrowthStage;
import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.model.*;
import com.agriscope.rule_engine.messaging.RecommendationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private SeedService seedService;

    @Autowired
    private RecommendationProducer recommendationProducer;

    public void evaluateCurrentDataForFarm(CurrentWeatherData weatherData, List<FieldDTO> fields, Map<String, Double> feedbackFactors) {
        log.info("Evaluating CURRENT rules for user {}, farm {}",
                weatherData.getUserId(), weatherData.getFarmId());

        KieSession kieSession = kieContainer.newKieSession();
        List<Recommendation> recommendations = new ArrayList<>();

        try {
            kieSession.setGlobal("recommendations", recommendations);
            kieSession.insert(weatherData);

            if (fields == null || fields.isEmpty()) {
                log.warn("No seeds defined for farm {}, skipping seed insertion", weatherData.getFarmId());
            } else {
                java.util.Set<SeedType> processedSeeds = new java.util.HashSet<>();
                for (FieldDTO field : fields) {
                    if (field.getSeed_type() == null) continue;

                    try {
                        SeedType type = SeedType.valueOf(field.getSeed_type().toUpperCase());
                        if (!processedSeeds.contains(type)) {
                            Seed originalSeed = seedService.getSeed(type);
                            if (originalSeed != null) {
                                Seed effectiveSeed = originalSeed.copy();
                                applyFeedbackAdjustments(effectiveSeed, feedbackFactors);
                                kieSession.insert(effectiveSeed);
                                processedSeeds.add(type);
                            }
                        }

                        GrowthStage stage = mapGrowthStage(field.getGrowth_stage());
                        kieSession.insert(new FieldStatus(field.getField_id(), type, stage));
                        log.debug("Inserted field status for current evaluation: {} - {}", field.getField_id(), stage);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown seed type received: {}", field.getSeed_type());
                    }
                }
            }

            int firedRules = kieSession.fireAllRules();
            log.info("Fired {} CURRENT rules", firedRules);

            processCurrentRecommendations(recommendations, weatherData);

        } finally {
            kieSession.dispose();
        }
    }

    public void evaluateHourlDataForFarm(List<HourlyWeatherData> hourlyData, FarmDetails farm, List<FieldDTO> fields) {
        if (hourlyData == null || hourlyData.isEmpty()) return;

        double sumEt0 = hourlyData.stream()
                .mapToDouble(d -> d.getEt0_fao_evapotranspiration() != null ? d.getEt0_fao_evapotranspiration() : 0.0)
                .sum();

        double sumRain = hourlyData.stream()
                .mapToDouble(d -> d.getPrecipitation() != null ? d.getPrecipitation() : 0.0)
                .sum();

        OptionalDouble avgTemperatureOpt = hourlyData.stream()
                .mapToDouble(d -> d.getTemperature_2m() != null ? d.getTemperature_2m() : 0.0)
                .average();
        double avgTemperature = avgTemperatureOpt.orElse(0.0);

        double currentMoisture = hourlyData.getFirst().getSoil_moisture_3_to_9cm();
        String userId = hourlyData.getFirst().getUserId();
        String email = hourlyData.getFirst().getEmail();

        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(sumEt0)
                .totalRain(sumRain)
                .currentSoilMoisture(currentMoisture)
                .build();

        List<Recommendation> recommendations = new ArrayList<>();

        if (fields != null) {
            for (FieldDTO field : fields) {
                if (field.getSeed_type() == null) continue;

                KieSession kieSession = kieContainer.newKieSession();
                try {
                    List<Recommendation> fieldSpecificRecs = new ArrayList<>();
                    kieSession.setGlobal("recommendations", fieldSpecificRecs);
                    kieSession.insert(farm);
                    kieSession.insert(analysis);

                    try {
                        SeedType type = SeedType.valueOf(field.getSeed_type().toUpperCase());
                        Seed originalSeed = seedService.getSeed(type);

                        if (originalSeed != null) {
                            Seed effectiveSeed = originalSeed.copy();
                            applyFeedbackAdjustments(effectiveSeed, farm.getFeedbackFactors());
                            kieSession.insert(effectiveSeed);
                        }

                        GrowthStage stage = mapGrowthStage(field.getGrowth_stage());
                        kieSession.insert(new FieldStatus(field.getField_id(), type, stage));

                        kieSession.fireAllRules();
                        if (!fieldSpecificRecs.isEmpty()) {
                            recommendations.add(fieldSpecificRecs.get(0));

                            if (fieldSpecificRecs.size() > 1) {
                                log.info("Filtered out {} lower priority rules for field {}",
                                        fieldSpecificRecs.size() - 1, field.getField_id());
                            }
                        }

                    } catch (Exception e) {
                        log.warn("Error processing field {}: {}", field.getField_id(), e.getMessage());
                    }

                } finally {
                    kieSession.dispose();
                }
            }
        }
        processHourlyRecommendations(recommendations, userId, email, farm.getFarmId(), avgTemperature);

    }

    private GrowthStage mapGrowthStage(String stageRaw) {
        if (stageRaw == null) return GrowthStage.YOUNG;

        try {
            return GrowthStage.valueOf(stageRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            switch (stageRaw) {
                case "0": return GrowthStage.SEEDLING;
                case "1": return GrowthStage.YOUNG;
                case "2": return GrowthStage.MATURE;
                case "3": return GrowthStage.READY;
                default:  return GrowthStage.MATURE;
            }
        }
    }

    private void processCurrentRecommendations(List<Recommendation> recommendations,
                                               CurrentWeatherData weatherData) {
        if (recommendations.isEmpty()) {
            log.info("No recommendations - conditions are normal");
            return;
        }

        for (Recommendation rec : recommendations) {
            rec.setUserId(weatherData.getUserId());
            rec.setEmail(weatherData.getEmail());
            rec.setFarmId(weatherData.getFarmId());
            rec.setWeatherTimestamp(weatherData.getTime());

            rec.getMetrics().put("temperature", weatherData.getTemperature_2m());

            log.info("Recommendation for user={}, farm={} : {} | Reason: {}",
                    rec.getUserId(), rec.getFarmId(), rec.getAdvice(), rec.getReasoning());

            recommendationProducer.sendRecommendation(rec);
        }
    }

    private void processHourlyRecommendations(List<Recommendation> recommendations, String userId, String email, String farmId, double avgTemperature) {
        if (recommendations.isEmpty()) {
            log.info("No recommendations - conditions are normal");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Recommendation rec : recommendations) {
            rec.setUserId(userId);
            rec.setEmail(email);
            rec.setFarmId(farmId);
            rec.setFieldId(rec.getFieldId() != null ? rec.getFieldId() : "N/A");
            log.info("Field id: {}", rec.getFieldId());
            rec.setWeatherTimestamp(now);

            rec.getMetrics().put("temperature", avgTemperature);

            log.info("Sending Recommendation: User={}, Advice={}", userId, rec.getAdvice());
            recommendationProducer.sendRecommendation(rec);
        }
    }

    public void evaluateDailyRules(List<DailyWeatherData> dailyList, List<FieldDTO> fields, Map<String, Double> feedbackFactors) {
        if (dailyList == null || dailyList.isEmpty()) return;

        KieSession kieSession = kieContainer.newKieSession();
        List<Recommendation> recommendations = new ArrayList<>();

        try {
            kieSession.setGlobal("recommendations", recommendations);

            for (DailyWeatherData day : dailyList) {
                kieSession.insert(day);
            }

            if (fields != null) {
                java.util.Set<SeedType> processedSeeds = new java.util.HashSet<>();
                for (FieldDTO field : fields) {
                    log.info("Processing field: {}", field);
                    log.info("Field seed: {}", field.getSeed_type());
                    if (field.getSeed_type() == null) {
                        continue;
                    }
                    try {
                        SeedType type = SeedType.valueOf(field.getSeed_type().toUpperCase());
                        if (!processedSeeds.contains(type)) {
                            Seed originalSeed = seedService.getSeed(type);
                            if (originalSeed != null) {
                                Seed effectiveSeed = originalSeed.copy();
                                applyFeedbackAdjustments(effectiveSeed, feedbackFactors);

                                kieSession.insert(effectiveSeed);
                                processedSeeds.add(type);
                            }
                        }
                        GrowthStage stage = mapGrowthStage(field.getGrowth_stage());
                        kieSession.insert(new FieldStatus(field.getField_id(), type, stage));
                    } catch (Exception e) {
                        log.warn("Invalid seed: {}", field.getSeed_type());
                    }
                }
            }

            int firedRules = kieSession.fireAllRules();
            log.info("Fired {} DAILY rules", firedRules);
            processDailyRecommendations(recommendations, dailyList.getFirst());

        } finally {
            kieSession.dispose();
        }
    }

    private void processDailyRecommendations(List<Recommendation> recs, DailyWeatherData metaData) {
        for (Recommendation rec : recs) {
            rec.setUserId(metaData.getUserId());
            rec.setEmail(metaData.getEmail());
            rec.setFarmId(metaData.getFarmId());
            rec.setWeatherTimestamp(LocalDateTime.now());
            log.info("Sending Daily Recommendation for target date: {}", rec.getMetrics().get("forecast_date"));
            recommendationProducer.sendRecommendation(rec);
        }
    }

    private void applyFeedbackAdjustments(Seed seed, Map<String, Double> factors) {
        if (factors == null || factors.isEmpty()) return;

        Map<String, Double> params = seed.getRuleParams();

        factors.forEach((paramName, multiplier) -> {
            if (params.containsKey(paramName)) {
                double originalValue = params.get(paramName);
                double adjustedValue = originalValue * multiplier;

                params.put(paramName, adjustedValue);

                log.info("Adjusted param {} for {} from {} to {} (multiplier: {})",
                        paramName, seed.getSeedType(), originalValue, adjustedValue, multiplier);
            }

            else if ("minTemperature".equals(paramName)) {
                seed.setMinTemperature(seed.getMinTemperature() * multiplier);
            }
            else if ("maxTemperature".equals(paramName) || "heatStressTemperature".equals(paramName)) {
                if (seed.getHeatStressTemperature() != null) {
                    seed.setHeatStressTemperature(seed.getHeatStressTemperature() * multiplier);
                }
            }
            else if ("allowedWaterDeficit".equals(paramName)) {
                if (seed.getAllowedWaterDeficit() != null) {
                    double newValue = seed.getAllowedWaterDeficit() * multiplier;
                    seed.setAllowedWaterDeficit(newValue);
                    seed.getRuleParams().put("allowedWaterDeficit", newValue);
                    log.info("Adjusted allowedWaterDeficit for {} (multiplier: {})", seed.getSeedType(), multiplier);
                }
            }
        });
    }
}