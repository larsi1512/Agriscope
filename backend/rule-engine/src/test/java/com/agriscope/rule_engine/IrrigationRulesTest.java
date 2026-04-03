package com.agriscope.rule_engine;

import com.agriscope.rule_engine.domain.dto.DailyAnalysis;
import com.agriscope.rule_engine.domain.enums.GrowthStage;
import com.agriscope.rule_engine.domain.enums.RecommendationType;
import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.enums.SoilType;
import com.agriscope.rule_engine.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IrrigationRulesTest {

    private KieContainer kieContainer;

    @BeforeEach
    void setUp() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        String[] ruleFiles = {
                "rules/irrigation/irrigation_common.drl",
                "rules/irrigation/white_grapes.drl",
                "rules/irrigation/wheat.drl",
                "rules/irrigation/pumpkin.drl",
                "rules/irrigation/corn.drl",
                "rules/irrigation/black_grapes.drl",
                "rules/irrigation/barley.drl"
        };

        for (String file : ruleFiles) {
            kfs.write(ks.getResources().newClassPathResource(file));
        }

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Drools Build Errors:\n" + kieBuilder.getResults().toString());
        }

        kieContainer = ks.newKieContainer(kieBuilder.getKieModule().getReleaseId());
    }


    // SCENARIO 1: CALCULATION CORE
    @Test
    @DisplayName("Verify Water Deficit Calculation Formula")
    void testWaterDeficitCalculation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Formula: Demand = TotalEt0 * (SeedKc * StageFactor)
        //          EffRain = TotalRain * SoilEfficiency
        //          Deficit = Demand - EffRain

        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        // Setup Data
        FarmDetails farm = new FarmDetails();
        farm.setSoilType(SoilType.LOAM);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.setSeedCoefficient(1.0);
        seed.setAllowedWaterDeficit(50.0);
        seed.setRuleParams(new HashMap<>());

        FieldStatus field = new FieldStatus("f1", SeedType.CORN, GrowthStage.MATURE);

        // Weather: Et0 = 10mm, Rain = 10mm
        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(10.0)
                .totalRain(10.0)
                .currentSoilMoisture(0.5)
                .build();

        session.insert(farm);
        session.insert(seed);
        session.insert(field);
        session.insert(analysis);

        session.fireAllRules();

        var objects = session.getObjects(o -> o.getClass().getSimpleName().equals("WaterDeficitResult"));
        assertEquals(1, objects.size(), "Should verify WaterDeficitResult was inserted");

        Object result = objects.iterator().next();

        double actualDeficit = (double) result.getClass().getMethod("getDeficit").invoke(result);

        double actualDemand = (double) result.getClass().getMethod("getPlantDemand").invoke(result);
        double actualEffRain = (double) result.getClass().getMethod("getEffectiveRain").invoke(result);

        // Expected Demand = 10.0 * (1.0 * 1.10) = 11.0
        // Expected EffRain = 10.0 * 0.85 = 8.5
        // Expected Deficit = 11.0 - 8.5 = 2.5
        assertEquals(11.0, actualDemand, 0.01, "Plant Demand calculation wrong");
        assertEquals(8.5, actualEffRain, 0.01, "Effective Rain calculation wrong");
        assertEquals(2.5, actualDeficit, 0.01, "Final Deficit calculation wrong");

        session.dispose();
    }


    // SCENARIO 2: HARVEST LOGIC (Stop Irrigation)
    static Stream<Arguments> harvestScenarios() {
        return Stream.of(
                Arguments.of(SeedType.CORN, "STOP Irrigation - Corn Harvest Approaching"),
                Arguments.of(SeedType.WHEAT, "STOP Irrigation - Wheat Harvest Approaching"),
                Arguments.of(SeedType.WHITE_GRAPES, "STOP Irrigation - Harvest Approaching"),
                Arguments.of(SeedType.BLACK_GRAPES, "STOP Irrigation - Harvest Approaching"),
                Arguments.of(SeedType.BARLEY, "STOP Irrigation - Barley Harvest Approaching"),
                Arguments.of(SeedType.PUMPKIN, "STOP Irrigation - Prevent Fruit Rot")
        );
    }

    @ParameterizedTest
    @MethodSource("harvestScenarios")
    void testHarvestStop_ShouldBlockIrrigation(SeedType type, String expectedMessage) {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(type);
        seed.setSeedCoefficient(1.0);
        seed.setAllowedWaterDeficit(10.0);
        seed.setMinSoilMoisture(0.3);
        seed.setRuleParams(new HashMap<>());

        // STAGE IS READY -> Key Condition
        FieldStatus field = new FieldStatus("f1", type, GrowthStage.READY);
        FarmDetails farm = new FarmDetails();
        farm.setSoilType(SoilType.LOAM);

        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(10.0) // creates demand
                .totalRain(0.0) // massive deficit
                .currentSoilMoisture(0.1) // dry soil
                .build();

        session.insert(seed);
        session.insert(field);
        session.insert(farm);
        session.insert(analysis);

        session.fireAllRules();

        assertFalse(recs.isEmpty());
        Recommendation rec = recs.getFirst();
        assertEquals(RecommendationType.READY_TO_HARVEST, rec.getRecommendationType());
        assertTrue(rec.getAdvice().contains("STOP"), "Should advise stopping irrigation");

        session.dispose();
    }


    // SCENARIO 3: URGENT IRRIGATION
    @Test
    @DisplayName("Logic: High Deficit + No Rain = IRRIGATE_NOW")
    void testUrgentIrrigation_Trigger() {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.setSeedCoefficient(1.5); // High water user
        seed.setAllowedWaterDeficit(10.0); // Low tolerance
        seed.setHeavyRainThreshold(20.0);
        seed.setMinSoilMoisture(0.3);
        seed.setRuleParams(new HashMap<>()); // default factor 0.9 for Corn Urgent

        FieldStatus field = new FieldStatus("f1", SeedType.CORN, GrowthStage.MATURE); // Factor 1.1
        FarmDetails farm = new FarmDetails();
        farm.setSoilType(SoilType.SILT); // Low efficiency

        // Demand = 10 * 1.5 * 1.1 = 16.5
        // Rain = 0.
        // Deficit = 16.5
        // Threshold = 10.0 * 0.9 = 9.0
        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(10.0)
                .totalRain(0.0)
                .currentSoilMoisture(0.2) // Low moisture
                .build();

        session.insert(seed);
        session.insert(field);
        session.insert(farm);
        session.insert(analysis);

        session.fireAllRules();

        boolean urgentFired = recs.stream()
                .anyMatch(r -> r.getRecommendationType() == RecommendationType.IRRIGATE_NOW);

        assertTrue(urgentFired, "Should recommend URGENT irrigation due to high deficit");
        session.dispose();
    }


    // SCENARIO 4: DELAY IRRIGATION (Rain Conflict)
    @Test
    @DisplayName("Priority: Heavy Rain Forecast should override Urgent Irrigation")
    void testRainForecast_OverridesUrgent() {
        // Scenario: Deficit is HIGH (Urgent), but Rain is HUGE (Delay).

        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.setAllowedWaterDeficit(5.0);
        seed.setHeavyRainThreshold(15.0);
        seed.setSeedCoefficient(1.0);
        seed.setMinSoilMoisture(0.3);
        seed.setRuleParams(new HashMap<>());

        FieldStatus field = new FieldStatus("f1", SeedType.CORN, GrowthStage.MATURE);
        FarmDetails farm = new FarmDetails();
        farm.setSoilType(SoilType.CLAY);

        // High Deficit (Et0 20) AND High Rain (25)
        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(60.0)
                .totalRain(30.0)
                .currentSoilMoisture(0.1)
                .build();

        session.insert(seed);
        session.insert(field);
        session.insert(farm);
        session.insert(analysis);

        session.fireAllRules();

        boolean delayFired = recs.stream()
                .anyMatch(r -> r.getRecommendationType() == RecommendationType.DELAY_IRRIGATION);

        assertTrue(delayFired, "Should advise DELAY because heavy rain is coming despite dry soil. Got: " +
                recs.stream().map(Recommendation::getRecommendationType).toList());
        session.dispose();
    }


    // SCENARIO 5: DYNAMIC PARAMETERS
    @Test
    @DisplayName("Logic: Increasing 'urgent_deficit_factor' should prevent alert")
    void testDynamicParam_SuppressesAlert() {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.BARLEY);
        seed.setAllowedWaterDeficit(10.0);
        seed.setSeedCoefficient(1.0);

        // Set tolerance factor to 2.0 (Huge tolerance)
        seed.setRuleParams(new HashMap<>());
        seed.getRuleParams().put("urgent_deficit_factor", 2.0);

        FieldStatus field = new FieldStatus("f1", SeedType.BARLEY, GrowthStage.MATURE);
        FarmDetails farm = new FarmDetails();
        farm.setSoilType(SoilType.LOAM);

        // Demand = 15. Deficit = 15.
        // 15 < 20 (Threshold). Should NOT fire Urgent.
        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(15.0)
                .totalRain(0.0)
                .currentSoilMoisture(0.5)
                .build();

        session.insert(seed);
        session.insert(field);
        session.insert(farm);
        session.insert(analysis);

        session.fireAllRules();

        boolean urgentFired = recs.stream()
                .anyMatch(r -> r.getRecommendationType() == RecommendationType.IRRIGATE_NOW);

        assertFalse(urgentFired, "Dynamic factor should have raised threshold preventing alert");
        session.dispose();
    }


    // SCENARIO 6: IRRIGATION SOON (Warning Zone)
    static Stream<Arguments> soonScenarios() {
        return Stream.of(
                Arguments.of(SeedType.CORN, 7.0, "Plan Irrigation Soon"),
                Arguments.of(SeedType.WHEAT, 8.0, "Plan Irrigation Soon"),
                Arguments.of(SeedType.PUMPKIN, 6.0, "Plan Irrigation Soon")
        );
    }

    @ParameterizedTest
    @MethodSource("soonScenarios")
    void testIrrigationSoon_Trigger(SeedType type, double et0Input, String expectedMsg) {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(type);
        seed.setSeedCoefficient(1.0);
        seed.setAllowedWaterDeficit(10.0); // Limit 10mm
        seed.setMinSoilMoisture(0.3);
        seed.setRuleParams(new HashMap<>());

        FieldStatus field = new FieldStatus("f1", type, GrowthStage.MATURE);
        FarmDetails farm = new FarmDetails();
        farm.setSoilType(SoilType.SILT); // Ef. 0.8

        // Deficit = (Et0 * 1.1) - 0.
        DailyAnalysis analysis = DailyAnalysis.builder()
                .totalEt0(et0Input)
                .totalRain(0.0)
                .currentSoilMoisture(0.3)
                .build();

        session.insert(seed);
        session.insert(field);
        session.insert(farm);
        session.insert(analysis);

        session.fireAllRules();

        boolean soonFired = recs.stream()
                .anyMatch(r -> r.getRecommendationType() == RecommendationType.IRRIGATE_SOON
                        || r.getRecommendationType() == RecommendationType.MONITOR_CONDITIONS);

        assertTrue(soonFired, "Should recommend IRRIGATION_SOON or MONITOR_CONDITIONS for " + type);
        session.dispose();
    }
}