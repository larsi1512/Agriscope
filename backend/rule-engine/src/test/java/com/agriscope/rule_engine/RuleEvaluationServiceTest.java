package com.agriscope.rule_engine;

import com.agriscope.rule_engine.domain.dto.DailyAnalysis;
import com.agriscope.rule_engine.domain.dto.FieldDTO;
import com.agriscope.rule_engine.domain.enums.GrowthStage;
import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.model.*;
import com.agriscope.rule_engine.messaging.RecommendationProducer;
import com.agriscope.rule_engine.service.RuleEvaluationService;
import com.agriscope.rule_engine.service.SeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEvaluationServiceTest {

    @Mock
    private KieContainer kieContainer;

    @Mock
    private KieSession kieSession;

    @Mock
    private SeedService seedService;

    @Mock
    private RecommendationProducer recommendationProducer;

    @InjectMocks
    private RuleEvaluationService ruleEvaluationService;

    @BeforeEach
    void setUp() {
        lenient().when(kieContainer.newKieSession()).thenReturn(kieSession);
    }

    // CURRENT DATA

    @Test
    void evaluateCurrentData_ShouldApplyFeedbackFactorsToSeed() {
        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setFarmId("farm1");

        FieldDTO field = new FieldDTO();
        field.setField_id("field1");
        field.setSeed_type("CORN");
        field.setGrowth_stage("MATURE");

        Map<String, Double> feedback = new HashMap<>();
        feedback.put("minTemperature", 1.10);

        Seed originalSeed = new Seed();
        originalSeed.setSeedType(SeedType.CORN);
        originalSeed.setMinTemperature(10.0);
        originalSeed.setRuleParams(new HashMap<>());

        when(seedService.getSeed(SeedType.CORN)).thenReturn(originalSeed);

        ruleEvaluationService.evaluateCurrentDataForFarm(weather, List.of(field), feedback);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kieSession, atLeastOnce()).insert(captor.capture());

        List<Object> insertedObjects = captor.getAllValues();

        Seed insertedSeed = insertedObjects.stream()
                .filter(obj -> obj instanceof Seed)
                .map(obj -> (Seed) obj)
                .findFirst()
                .orElseThrow();

        //  10.0 * 1.10 = 11.0?
        assertEquals(11.0, insertedSeed.getMinTemperature(), 0.01);

        boolean statusExists = insertedObjects.stream()
                .anyMatch(obj -> obj instanceof FieldStatus && ((FieldStatus)obj).getFieldId().equals("field1"));
        assertTrue(statusExists);

        verify(kieSession).fireAllRules();
        verify(kieSession).dispose();
    }

    @Test
    void evaluateCurrentData_ShouldHandleUnknownSeedTypeGracefully() {
        CurrentWeatherData weather = new CurrentWeatherData();
        FieldDTO field = new FieldDTO();
        field.setSeed_type("UNKNOWN_PLANT");

        ruleEvaluationService.evaluateCurrentDataForFarm(weather, List.of(field), new HashMap<>());

        verify(seedService, never()).getSeed(any());
        verify(kieSession, never()).insert(any(Seed.class));
        verify(kieSession).fireAllRules();
    }

    @Test
    void evaluateCurrentData_ShouldSendRecommendation_WhenRulesFire() {
        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setUserId("user1");
        weather.setFarmId("farm1");

        doAnswer(invocation -> {
            List<Recommendation> globalList = invocation.getArgument(1);
            Recommendation rec = new Recommendation();
            rec.setAdvice("Irrigate now");
            rec.setMetrics(new HashMap<>());
            globalList.add(rec);
            return null;
        }).when(kieSession).setGlobal(eq("recommendations"), anyList());

        ruleEvaluationService.evaluateCurrentDataForFarm(weather, new ArrayList<>(), new HashMap<>());

        verify(recommendationProducer).sendRecommendation(any(Recommendation.class));
    }

    // HOURLY DATA

    @Test
    void evaluateHourlyData_ShouldCalculateAggregatesCorrectly() {
        HourlyWeatherData h1 = new HourlyWeatherData();
        h1.setEt0_fao_evapotranspiration(0.5);
        h1.setPrecipitation(2.0);
        h1.setTemperature_2m(20.0);
        h1.setSoil_moisture_3_to_9cm(30.0);
        h1.setUserId("user1");

        HourlyWeatherData h2 = new HourlyWeatherData();
        h2.setEt0_fao_evapotranspiration(0.5);
        h2.setPrecipitation(0.0);
        h2.setTemperature_2m(30.0);

        List<HourlyWeatherData> hourlyList = List.of(h1, h2);
        FarmDetails farm = new FarmDetails();
        farm.setFarmId("farm1");

        FieldDTO dummyField = new FieldDTO();
        dummyField.setSeed_type("CORN");
        when(seedService.getSeed(any())).thenReturn(new Seed());

        ruleEvaluationService.evaluateHourlDataForFarm(hourlyList, farm, List.of(dummyField));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kieSession, atLeastOnce()).insert(captor.capture());

    }

    @Test
    void evaluateHourlyData_FullFlow_WithFields() {
        HourlyWeatherData h1 = new HourlyWeatherData();
        h1.setUserId("u1"); h1.setTemperature_2m(20.0); h1.setSoil_moisture_3_to_9cm(25.0);
        HourlyWeatherData h2 = new HourlyWeatherData();
        h2.setUserId("u1"); h2.setTemperature_2m(30.0);

        FieldDTO field = new FieldDTO();
        field.setField_id("f1");
        field.setSeed_type("WHEAT");
        field.setGrowth_stage("1");

        when(seedService.getSeed(SeedType.WHEAT)).thenReturn(new Seed());

        ruleEvaluationService.evaluateHourlDataForFarm(List.of(h1, h2), new FarmDetails(), List.of(field));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kieSession, atLeastOnce()).insert(captor.capture());
        List<Object> allInserts = captor.getAllValues();

        DailyAnalysis analysis = allInserts.stream()
                .filter(o -> o instanceof DailyAnalysis)
                .map(o -> (DailyAnalysis) o)
                .findFirst().orElseThrow();

        assertEquals(25.0, analysis.getCurrentSoilMoisture());

        FieldStatus status = allInserts.stream()
                .filter(o -> o instanceof FieldStatus)
                .map(o -> (FieldStatus) o)
                .findFirst().orElseThrow();
        assertEquals(GrowthStage.YOUNG, status.getStage());
    }

    @Test
    void evaluateHourlyData_ShouldProcessAndSendRecommendations() {
        HourlyWeatherData h1 = new HourlyWeatherData();
        h1.setUserId("u1");
        h1.setEmail("test@mail.com");
        h1.setTemperature_2m(20.0);
        h1.setSoil_moisture_3_to_9cm(20.0);

        FarmDetails farm = new FarmDetails();
        farm.setFarmId("farm1");

        FieldDTO field = new FieldDTO();
        field.setField_id("f1");
        field.setSeed_type("CORN");
        field.setGrowth_stage("1");

        when(seedService.getSeed(any())).thenReturn(new Seed());

        doAnswer(invocation -> {
            List<Recommendation> list = invocation.getArgument(1);
            Recommendation rec = new Recommendation();
            rec.setAdvice("Hourly Irrigation Needed");
            rec.setMetrics(new HashMap<>());
            rec.setFieldId("f1");
            list.add(rec);
            return null;
        }).when(kieSession).setGlobal(eq("recommendations"), anyList());

        ruleEvaluationService.evaluateHourlDataForFarm(List.of(h1), farm, List.of(field));

        verify(recommendationProducer).sendRecommendation(argThat(rec ->
                rec.getAdvice().equals("Hourly Irrigation Needed") &&
                        rec.getUserId().equals("u1") &&
                        rec.getFieldId().equals("f1")
        ));
    }
    // DAILY DATA

    @Test
    void evaluateDailyRules_ShouldInsertDailyDataAndSeeds() {
        DailyWeatherData d1 = new DailyWeatherData();
        d1.setTemperature_2m_max(30.0);
        d1.setUserId("user1");
        DailyWeatherData d2 = new DailyWeatherData();
        d2.setTemperature_2m_max(32.0);

        List<DailyWeatherData> dailyList = List.of(d1, d2);

        FieldDTO field = new FieldDTO();
        field.setField_id("f1");
        field.setSeed_type("WHEAT");
        field.setGrowth_stage("MATURE");

        Seed wheatSeed = new Seed();
        wheatSeed.setSeedType(SeedType.WHEAT);
        when(seedService.getSeed(SeedType.WHEAT)).thenReturn(wheatSeed);

        ruleEvaluationService.evaluateDailyRules(dailyList, List.of(field), new HashMap<>());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // Expected 2 daily + 1 seed + 1 fieldStatus = 4 inserts
        verify(kieSession, atLeast(4)).insert(captor.capture());

        List<Object> inserted = captor.getAllValues();

        long dailyCount = inserted.stream().filter(o -> o instanceof DailyWeatherData).count();
        assertEquals(2, dailyCount);

        boolean seedExists = inserted.stream().anyMatch(o -> o instanceof Seed);
        assertTrue(seedExists);

        verify(kieSession).fireAllRules();
    }

    @Test
    void evaluateDailyRules_ShouldProcessAndSendRecommendations() {
        DailyWeatherData d1 = new DailyWeatherData();
        d1.setUserId("u1");
        d1.setEmail("mail@test.com");
        d1.setFarmId("farm1");

        doAnswer(invocation -> {
            List<Recommendation> list = invocation.getArgument(1);
            Recommendation rec = new Recommendation();
            rec.setAdvice("Daily Frost Warning");
            rec.setMetrics(new HashMap<>());
            list.add(rec);
            return null;
        }).when(kieSession).setGlobal(eq("recommendations"), anyList());

        ruleEvaluationService.evaluateDailyRules(List.of(d1), new ArrayList<>(), new HashMap<>());

        verify(recommendationProducer).sendRecommendation(argThat(rec ->
                rec.getAdvice().equals("Daily Frost Warning") &&
                        rec.getUserId().equals("u1")
        ));
    }


    // Test helper methods

    @Test
    void mapGrowthStage_ShouldHandleNumericStrings() {
        FieldDTO f1 = new FieldDTO(); f1.setSeed_type("CORN"); f1.setGrowth_stage("0");

        when(seedService.getSeed(any())).thenReturn(new Seed());

        ruleEvaluationService.evaluateCurrentDataForFarm(new CurrentWeatherData(), List.of(f1), new HashMap<>());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kieSession, atLeastOnce()).insert(captor.capture());

        FieldStatus status = captor.getAllValues().stream()
                .filter(o -> o instanceof FieldStatus)
                .map(o -> (FieldStatus) o)
                .findFirst().orElseThrow();

        assertEquals(GrowthStage.SEEDLING, status.getStage());
    }

    @Test
    void applyFeedbackAdjustments_ShouldUpdateRuleParams() {
        CurrentWeatherData weather = new CurrentWeatherData();
        FieldDTO field = new FieldDTO();
        field.setSeed_type("CORN");

        Map<String, Double> factors = Map.of("kc_factor", 1.5);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.getRuleParams().put("kc_factor", 2.0);

        when(seedService.getSeed(SeedType.CORN)).thenReturn(seed);

        ruleEvaluationService.evaluateCurrentDataForFarm(weather, List.of(field), factors);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kieSession, atLeastOnce()).insert(captor.capture());

        Seed capturedSeed = captor.getAllValues().stream()
                .filter(o -> o instanceof Seed).map(o -> (Seed)o).findFirst().orElseThrow();

        assertEquals(3.0, capturedSeed.getRuleParams().get("kc_factor"), 0.01);
    }

    @Test
    void applyFeedbackAdjustments_ShouldCoverAllBranches() {
        CurrentWeatherData weather = new CurrentWeatherData();
        FieldDTO field = new FieldDTO();
        field.setGrowth_stage("YOUNG");
        field.setSeed_type("CORN");
        field.setField_id("f1");

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.setMinTemperature(10.0);
        seed.setHeatStressTemperature(30.0);
        seed.setAllowedWaterDeficit(0.5);
        seed.setRuleParams(new HashMap<>());
        seed.getRuleParams().put("genericParam", 100.0);

        when(seedService.getSeed(SeedType.CORN)).thenReturn(seed);

        Map<String, Double> factors = Map.of(
                "minTemperature", 1.1,         // increase min temp 10% -> 11.0
                "maxTemperature", 0.9,         // lower heat stress 10% -> 27.0
                "allowedWaterDeficit", 1.2,    // increase deficit  20% -> 0.6
                "genericParam", 2.0,           // generic parametar -> 200.0
                "nonExistentParam", 5.0        // should not break
        );

        ruleEvaluationService.evaluateCurrentDataForFarm(weather, List.of(field), factors);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kieSession, atLeastOnce()).insert(captor.capture());

        Seed insertedSeed = captor.getAllValues().stream()
                .filter(o -> o instanceof Seed)
                .map(o -> (Seed) o)
                .findFirst().orElseThrow();

        assertEquals(11.0, insertedSeed.getMinTemperature(), 0.01, "Min Temp not scaled");
        assertEquals(27.0, insertedSeed.getHeatStressTemperature(), 0.01, "Heat Stress not scaled");
        assertEquals(0.6, insertedSeed.getAllowedWaterDeficit(), 0.01, "Water Deficit not scaled");
        assertEquals(200.0, insertedSeed.getRuleParams().get("genericParam"), 0.01, "Generic param not scaled");
    }
}