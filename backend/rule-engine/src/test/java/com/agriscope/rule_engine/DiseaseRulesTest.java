package com.agriscope.rule_engine;

import com.agriscope.rule_engine.domain.enums.GrowthStage;
import com.agriscope.rule_engine.domain.enums.SeedType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseRulesTest {

    private KieContainer kieContainer;

    @BeforeEach
    void setUp() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        String[] ruleFiles = {
                "rules/disease/white_grapes.drl",
                "rules/disease/wheat.drl",
                "rules/disease/pumpkin.drl",
                "rules/disease/corn.drl",
                "rules/disease/black_grapes.drl",
                "rules/disease/barley.drl"
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

    // SCENARIO 1: HAPPY PATH (Disease & Pest Triggers)
    static Stream<Arguments> diseaseScenarios() {
        return Stream.of(
                // 1. WHITE GRAPES (Downy Mildew: Wet & Warm)
                // Rule: rain > (threshold * factor) AND temp_min > (min + offset)
                Arguments.of(SeedType.WHITE_GRAPES, 20.0, 15.0, 0.0, "7-Day Forecast: Downy Mildew Risk on"),

                // 2. WHITE GRAPES (Berry Moth: Dry & Warm)
                // Rule: rain < 2.0 AND temp_min > 14.0
                Arguments.of(SeedType.WHITE_GRAPES, 0.0, 16.0, 0.0, "7-Day Forecast: Berry Moth Risk on"),

                // 3. WHEAT (Fusarium: Wet & Warm)
                // Rule: rain > limit AND temp_max >= min AND temp_max <= max
                Arguments.of(SeedType.WHEAT, 15.0, 25.0, 0.0, "7-Day Forecast Alert: Fusarium Risk on"),

                // 4. PUMPKIN (Fruit Rot: Heavy Rain)
                // Rule: rain > heavyRainThreshold
                Arguments.of(SeedType.PUMPKIN, 50.0, 20.0, 0.0, "7-Day Forecast: Fruit Rot Risk on"),

                // 5. CORN (Leaf Blight: Wet & Warm)
                // Rule: rain > limit AND temp_min > limit
                Arguments.of(SeedType.CORN, 15.0, 20.0, 0.0, "Monitor for Leaf Blight"),

                // 6. CORN (Wind Damage)
                // Rule: wind > limit
                Arguments.of(SeedType.CORN, 0.0, 20.0, 80.0, "7-Day Forecast: High Wind Alert on"),

                // 7. BLACK GRAPES (Botrytis: Wet & Warm)
                Arguments.of(SeedType.BLACK_GRAPES, 20.0, 25.0, 0.0, "7-Day Forecast: High Botrytis Risk on"),

                // 8. BARLEY (Net Blotch: Wet & Cool)
                // Rule: rain > limit AND temp_max < limit
                Arguments.of(SeedType.BARLEY, 10.0, 15.0, 0.0, "Disease Risk: Net Blotch"),

                // 9. WHEAT Nutrient Loss (Heavy Rain)
                // Rule: rain > heavyRainThreshold (20.0 default)
                Arguments.of(SeedType.WHEAT, 25.0, 20.0, 0.0, "Nutrient Leaching Risk"),

                // 10. PUMPKIN Powdery Mildew (Dry & Warm) ---
                // Rule: rain < dryLimit (2.0) AND temp > min(10.0) AND temp < max (35.0)
                Arguments.of(SeedType.PUMPKIN, 0.0, 25.0, 0.0, "Scout for Powdery Mildew"),

                // 11. BLACK GRAPES Sun Scald (Extreme Heat) ---
                // Rule: temp_max > heatStress (30.0)
                Arguments.of(SeedType.BLACK_GRAPES, 0.0, 35.0, 0.0, "Sun Scald / Heat Stress")
        );
    }

    @ParameterizedTest(name = "{0}: Rain={1}, Temp={2}, Wind={3} -> Expects {4}")
    @MethodSource("diseaseScenarios")
    void testDiseaseTriggers_HappyPath(SeedType type, double rain, double temp, double wind, String expectedAdviceFragment) {
        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        //  Configure Seed (Default values)
        Seed seed = new Seed();
        seed.setSeedType(type);
        seed.setDiseaseRainThreshold(5.0);
        seed.setDiseaseRiskMinTemp(10.0);
        seed.setDiseaseRiskMaxTemp(35.0);
        seed.setHeavyRainThreshold(20.0);
        seed.setMaxWindTolerance(50.0);
        seed.setHeatStressTemperature(30.0);
        seed.setRuleParams(new HashMap<>()); // Empty params = default behavior

        //  Configure Field
        FieldStatus field = new FieldStatus("f1", type, GrowthStage.MATURE);

        //  Configure Daily Weather
        DailyWeatherData day = new DailyWeatherData();
        day.setUserId("u1");
        day.setDate(java.time.LocalDateTime.now());
        day.setRain_sum(rain);
        day.setTemperature_2m_max(temp); // Used by Wheat, Pumpkin, Barley, Black Grapes
        day.setTemperature_2m_min(temp); // Used by White Grapes, Corn, Berry Moth
        day.setWind_speed_10m_max(wind); // Used by Corn, Barley

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        session.fireAllRules();

        assertFalse(recs.isEmpty(), "Rule should have fired for " + type);
        boolean matchFound = recs.stream()
                .anyMatch(r -> r.getAdvice().contains(expectedAdviceFragment));

        assertTrue(matchFound,
                "Expected advice containing '" + expectedAdviceFragment + "' but got list: " +
                        recs.stream().map(Recommendation::getAdvice).toList());
        session.dispose();
    }


    // SCENARIO 2: DYNAMIC PARAMETERS
    @Test
    @DisplayName("Logic: Modifying 'disease_rain_factor' should change the trigger threshold")
    void testDynamicParameters_AdjustThresholds() {
        // Scenario: Wheat Fusarium.
        // Default Rain Threshold = 5.0.
        // We set 'disease_rain_factor' to 2.0 -> New Threshold = 10.0.
        // We input Rain = 8.0.
        // Result: Should NOT fire (because 8.0 < 10.0), even though it is > default 5.0.

        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHEAT);
        seed.setDiseaseRainThreshold(5.0);
        seed.setDiseaseRiskMinTemp(10.0);
        seed.setDiseaseRiskMaxTemp(30.0);
        // DYNAMIC PARAMETER
        seed.setRuleParams(new HashMap<>());
        seed.getRuleParams().put("disease_rain_factor", 2.0); // Doubling the threshold

        FieldStatus field = new FieldStatus("f1", SeedType.WHEAT, GrowthStage.MATURE);

        DailyWeatherData day = new DailyWeatherData();
        day.setRain_sum(8.0); // High rain, but below the adjusted threshold (10.0)
        day.setTemperature_2m_max(20.0); // Favorable temp

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Rule fired despite dynamic parameter raising the threshold");
        session.dispose();
    }

    @Test
    @DisplayName("Logic: 'pest_temp_offset' should shift the temperature requirement")
    void testDynamicParameters_TempOffset() {
        // Scenario: White Grapes Berry Moth
        // Base Temp Requirement: > 14.0 C
        // Offset: +5.0 C
        // Effective Limit: > 19.0 C
        // Actual Temp: 18.0 C
        // Result: Should NOT fire (18 < 19)

        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHITE_GRAPES);
        seed.setRuleParams(new HashMap<>());
        seed.getRuleParams().put("pest_temp_offset", 5.0);

        FieldStatus field = new FieldStatus("f1", SeedType.WHITE_GRAPES, GrowthStage.MATURE);

        DailyWeatherData day = new DailyWeatherData();
        day.setTemperature_2m_min(18.0); // not warm enough due to offset
        day.setRain_sum(0.0); // Dry (required condition)

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        session.fireAllRules();

        assertTrue(recs.isEmpty(), "Pest alert fired but temp was below adjusted threshold");
        session.dispose();
    }


    // SCENARIO 3: COMPLEX CONDITIONS
    @Test
    @DisplayName("Complex: Barley Lodging requires BOTH Rain AND Wind")
    void testBarleyLodging_RequiresBothConditions() {
        // Rule: Rain > Limit AND Wind > Limit
        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.BARLEY);
        seed.setMaxWindTolerance(50.0);
        seed.setRuleParams(new HashMap<>());
        // Default param values in DRL: lodging_rain_limit = 5.0, lodging_wind_factor = 0.8 (Limit = 40.0)

        FieldStatus field = new FieldStatus("f1", SeedType.BARLEY, GrowthStage.MATURE);

        DailyWeatherData day = new DailyWeatherData();
        day.setRain_sum(10.0); // Rain condition MET
        day.setWind_speed_10m_max(20.0); // Wind condition not met(20 < 40)

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Lodging rule fired with only Rain but no Wind");
        session.dispose();
    }


    // SCENARIO 4: EDGE CASES / BOUNDARIES
    @Test
    @DisplayName("Boundary: Wheat Disease uses '>=' for min temp, others use '>'")
    void testBoundary_WheatInclusiveMinTemp() {
        // Wheat rule: temperature_2m_max >= $minTemp
        // Setup: MinTemp = 15.0. Actual Temp = 15.0.
        // Result: Should FIRE.

        KieSession session = kieContainer.newKieSession();
        List<Recommendation> recs = new ArrayList<>();
        session.setGlobal("recommendations", recs);

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHEAT);
        seed.setDiseaseRainThreshold(0.0); // Rain always passes
        seed.setDiseaseRiskMinTemp(15.0);
        seed.setDiseaseRiskMaxTemp(30.0);

        FieldStatus field = new FieldStatus("f1", SeedType.WHEAT, GrowthStage.MATURE);

        DailyWeatherData day = new DailyWeatherData();
        day.setDate(java.time.LocalDateTime.now());
        day.setRain_sum(10.0);
        day.setTemperature_2m_max(15.0); // EXACTLY ON BOUNDARY

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        int fired = session.fireAllRules();

        assertEquals(1, fired, "Wheat rule should fire on inclusive boundary (>=)");
        session.dispose();
    }

    @Test
    @DisplayName("Boundary: Disease should NOT fire if temperature exceeds the Maximum limit")
    void testDisease_UpperBoundTemperature() {
        // Scenario: Wheat Fusarium logic includes: temperature_2m_max <= diseaseRiskMaxTemp
        // MaxTemp default is 30.0
        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHEAT);
        seed.setDiseaseRainThreshold(0.0); // Rain condition OK
        seed.setDiseaseRiskMinTemp(10.0);  // Min Temp OK
        seed.setDiseaseRiskMaxTemp(30.0);  // MAX LIMIT

        FieldStatus field = new FieldStatus("f1", SeedType.WHEAT, GrowthStage.MATURE);

        DailyWeatherData day = new DailyWeatherData();
        day.setRain_sum(10.0);
        day.setTemperature_2m_max(31.0); // TOO HOT Should block disease.

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Disease rule fired even though temperature was above the Max Risk threshold");
        session.dispose();
    }

    @Test
    @DisplayName("Logic: Dry-weather pests should NOT trigger if it rains")
    void testPestRisk_RainSuppression() {
        // Scenario: White Grapes Berry Moth requires rain_sum < 2.0 (default)
        // If rain is 3.0, it should NOT fire.

        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.WHITE_GRAPES);
        seed.setRuleParams(new HashMap<>()); // Default param "pest_dry_rain_limit" is 2.0

        FieldStatus field = new FieldStatus("f1", SeedType.WHITE_GRAPES, GrowthStage.MATURE);

        DailyWeatherData day = new DailyWeatherData();
        day.setTemperature_2m_min(20.0); // Warm enough
        day.setRain_sum(3.0); // Too wet for this pest

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Dry-weather pest rule fired despite rain exceeding the limit");
        session.dispose();
    }


    // SCENARIO 5: NEGATIVE TEST
    @Test
    @DisplayName("Logic: Diseases should be ignored if crop is READY (Harvested)")
    void testGrowthStageReady_Ignored() {
        KieSession session = kieContainer.newKieSession();
        session.setGlobal("recommendations", new ArrayList<>());

        Seed seed = new Seed();
        seed.setSeedType(SeedType.CORN);
        seed.setDiseaseRainThreshold(0.0); // Always satisfy rain condition
        seed.setDiseaseRiskMinTemp(0.0);   // Always satisfy temp condition

        // Field is READY -> No disease logic should apply
        FieldStatus field = new FieldStatus("f1", SeedType.CORN, GrowthStage.READY);

        DailyWeatherData day = new DailyWeatherData();
        day.setRain_sum(100.0);
        day.setTemperature_2m_min(20.0);

        session.insert(seed);
        session.insert(field);
        session.insert(day);

        int fired = session.fireAllRules();

        assertEquals(0, fired, "Disease rules should not fire for READY crops");
        session.dispose();
    }
}