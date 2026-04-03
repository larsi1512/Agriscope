package com.agriscope.rule_engine.rules;

import com.agriscope.rule_engine.domain.enums.GrowthStage;
import com.agriscope.rule_engine.domain.enums.RecommendationType;
import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SafetyRulesTest {

    private KieContainer kieContainer;

    @BeforeEach
    void setUp() {
        KieServices ks = KieServices.Factory.get();
        org.kie.api.builder.KieFileSystem kfs = ks.newKieFileSystem();
        String[] ruleFiles = {
                "rules/safety/barley.drl",
                "rules/safety/corn.drl",
                "rules/safety/wheat.drl",
                "rules/safety/pumpkin.drl",
                "rules/safety/white_grapes.drl",
                "rules/safety/black_grapes.drl"
        };
        for (String file : ruleFiles) {
            kfs.write(ks.getResources().newClassPathResource(file));
        }

        org.kie.api.builder.KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();

        if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new RuntimeException("Incorrect .drl files:\n" + kieBuilder.getResults().toString());
        }

        kieContainer = ks.newKieContainer(kieBuilder.getKieModule().getReleaseId());
    }


    // SCENARIO 1: Parameterized Test for FROST ALERTS
     // Data Source for Frost Tests
    // SeedType | MinTemp Threshold | Actual Temp | Expected Advice string
    static Stream<Arguments> frostScenarios() {
        return Stream.of(
                Arguments.of(SeedType.WHITE_GRAPES, 0.0, -1.0, "IGNITE_FROST_CANDLES_OR_IRRIGATE"),
                Arguments.of(SeedType.WHEAT,        -2.0, -5.0, "INSPECT_FIELDS_FOR_FROST_DAMAGE"),
                Arguments.of(SeedType.PUMPKIN,      5.0,  2.0,  "COVER_WITH_FLEECE_IMMEDIATELY"),
                Arguments.of(SeedType.CORN,         4.0,  0.0,  "CHECK_GROWING_POINT_RECOVERY"),
                Arguments.of(SeedType.BLACK_GRAPES, 0.0, -2.5,  "IGNITE_FROST_CANDLES_OR_IRRIGATE"),
                Arguments.of(SeedType.BARLEY,       -3.0, -10.0,"INSPECT_FIELDS_FOR_FROST_DAMAGE")
        );
    }

    @ParameterizedTest(name = "{0}: Temp {2} < Threshold {1} should trigger {3}")
    @MethodSource("frostScenarios")
    void testFrostAlerts_ShouldFireRule(SeedType type, double minThreshold, double actualTemp, String expectedAdvice) {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recommendations = new ArrayList<>();
        session.setGlobal("recommendations", recommendations);

        Seed seed = new Seed();
        seed.setSeedType(type);
        seed.setMinTemperature(minThreshold);

        FieldStatus field = new FieldStatus("field1", type, GrowthStage.YOUNG); // Not READY

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(actualTemp); // Below threshold
        weather.setUserId("user1");
        weather.setTime(LocalDateTime.now());

        session.insert(seed);
        session.insert(field);
        session.insert(weather);
        int fired = session.fireAllRules();
        session.dispose();

        assertEquals(1, fired, "Exactly one rule should fire for " + type);
        assertEquals(1, recommendations.size());

        Recommendation rec = recommendations.get(0);
        assertEquals(RecommendationType.FROST_ALERT, rec.getRecommendationType());
        assertEquals(expectedAdvice, rec.getAdvice());
        assertEquals(type, rec.getRecommendedSeed());
    }

    // SCENARIO 2: Parameterized Test for HEAT ALERTS
    static Stream<Arguments> heatScenarios() {
        return Stream.of(
                Arguments.of(SeedType.WHITE_GRAPES, 30.0, 35.0, "MANAGE_CANOPY_AND_IRRIGATE"),
                Arguments.of(SeedType.CORN,         32.0, 33.0, "IRRIGATE_TO_SAVE_POLLINATION"),
                Arguments.of(SeedType.WHEAT,        30.0, 32.0, "START_MOBILE_IRRIGATION"),
                Arguments.of(SeedType.PUMPKIN,      28.0, 30.0, "IRRIGATE_TO_PREVENT_FLOWER_DROP"),
                Arguments.of(SeedType.BLACK_GRAPES, 30.0, 35.0, "MANAGE_CANOPY_SHADE"),
                Arguments.of(SeedType.BARLEY,       25.0, 30.0, "START_MOBILE_IRRIGATION")
        );
    }

    @ParameterizedTest
    @MethodSource("heatScenarios")
    void testHeatAlerts_ShouldFireRule(SeedType type, double maxThreshold, double actualTemp, String expectedAdvice) {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recommendations = new ArrayList<>();
        session.setGlobal("recommendations", recommendations);

        Seed seed = new Seed();
        seed.setSeedType(type);
        seed.setHeatStressTemperature(maxThreshold);

        FieldStatus field = new FieldStatus("field1", type, GrowthStage.MATURE);

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(actualTemp);

        session.insert(seed);
        session.insert(field);
        session.insert(weather);
        session.fireAllRules();
        session.dispose();

        assertFalse(recommendations.isEmpty(), "Heat alert should be generated");
        assertEquals(RecommendationType.HEAT_ALERT, recommendations.get(0).getRecommendationType());
        assertEquals(expectedAdvice, recommendations.get(0).getAdvice());
    }

    // SCENARIO 3: EDGE CASES
    @Test
    @DisplayName("Boundary: Rule should NOT fire if temperature equals the threshold exactly")
    void testBoundaryConditions_ExactThreshold() {

        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.setMinTemperature(5.0);

        FieldStatus field = new FieldStatus("f1", SeedType.CORN, GrowthStage.YOUNG);

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(5.0); // exactly on threshold

        session.insert(seed);
        session.insert(field);
        session.insert(weather);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Rule should strictly use < operator, so equal values must not fire");
        session.dispose();
    }

    // SCENARIO 4: LOGIC FILTERING (Growth Stage)
    @Test
    @DisplayName("Logic: Alerts should be suppressed if Growth Stage is READY")
    void testGrowthStageFiltering() {

        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHEAT);
        seed.setMinTemperature(0.0);

        // set stage to READY - seeds are usually not frost-sensitive anymore
        FieldStatus field = new FieldStatus("f1", SeedType.WHEAT, GrowthStage.READY);

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(-5.0); // Freezing

        session.insert(seed);
        session.insert(field);
        session.insert(weather);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Rule should not fire when stage is READY");
        session.dispose();
    }

    // SCENARIO 5: ISOLATION (Cross Contamination)
    @Test
    @DisplayName("Isolation: Corn rules should not fire for Wheat objects")
    void testTypeIsolation() {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        // insert WHEAT configuration
        Seed wheatSeed = new Seed();
        wheatSeed.setSeedType(SeedType.WHEAT);
        wheatSeed.setMinTemperature(0.0);

        // insert CORN field status (Mismatch!)
        FieldStatus cornField = new FieldStatus("f1", SeedType.CORN, GrowthStage.YOUNG);

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(-5.0);

        session.insert(wheatSeed);
        session.insert(cornField); // Mismatched type
        session.insert(weather);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Wheat rule should not match Corn field");
        session.dispose();
    }

    // SCENARIO 6: HEAT BOUNDARY (Exact Threshold)
    @Test
    @DisplayName("Boundary: Heat Rule should NOT fire if temperature equals the threshold exactly")
    void testBoundaryConditions_HeatThreshold() {

        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHEAT);
        seed.setHeatStressTemperature(30.0);

        FieldStatus field = new FieldStatus("f1", SeedType.WHEAT, GrowthStage.MATURE);

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(30.0); // EXACTLY ON THRESHOLD

        session.insert(seed);
        session.insert(field);
        session.insert(weather);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Heat rule should strictly use > operator, so equal values must not fire");
        session.dispose();
    }

    // SCENARIO 7: MIXED FIELDS (Partial Firing)
    @Test
    @DisplayName("Complex: With two fields of same type, only the valid one should trigger alert")
    void testMixedFieldStages() {

        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed wheatSeed = new Seed();
        wheatSeed.setSeedType(SeedType.WHEAT);
        wheatSeed.setMinTemperature(0.0);

        FieldStatus fieldA = new FieldStatus("Field_A", SeedType.WHEAT, GrowthStage.YOUNG);
        FieldStatus fieldB = new FieldStatus("Field_B", SeedType.WHEAT, GrowthStage.READY);

        CurrentWeatherData weather = new CurrentWeatherData();
        weather.setTemperature_2m(-5.0);
        weather.setUserId("user1");

        session.insert(wheatSeed);
        session.insert(fieldA);
        session.insert(fieldB);
        session.insert(weather);

        int fired = session.fireAllRules();

        assertEquals(1, fired, "Should fire only for the eligible field");
        assertEquals(1, recs.size());

        session.dispose();
    }
}