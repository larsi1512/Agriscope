package ase_pr_inso_01.farm_service.service;
import org.springframework.test.util.ReflectionTestUtils;

import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.Seed;
import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.models.enums.GrowthStage;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.repository.SeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class GrowthStageSchedulerTests {

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private SeedRepository seedRepository;

    @Autowired
    private GrowthStageScheduler scheduler;

    @MockitoBean
    private TaskScheduler taskScheduler;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        farmRepository.deleteAll();
        seedRepository.deleteAll();
    }

    @Test
    void shouldUpdateStageToReady() {
        String farmId = "farm-1";
        SeedType seedType = SeedType.BARLEY;

        Seed barleySeed = new Seed();
        barleySeed.setSeedType(seedType);
        barleySeed.setDaysToYoung(2);
        barleySeed.setDaysToMature(5);
        barleySeed.setDaysToReady(10);
        seedRepository.save(barleySeed);

        Field field = new Field();
        field.setId(1);
        field.setSeedType(seedType);
        field.setStatus(FieldStatus.PLANTED);
        field.setGrowthStage(GrowthStage.SEEDLING);

        LocalDate elevenDaysAgo = LocalDate.now().minusDays(11);
        field.setPlantedDate(Date.from(elevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        scheduler.updateGrowthStages();

        Farm updatedFarm = farmRepository.findById(farmId)
                .orElseThrow(() -> new AssertionError("Farm not found in DB"));

        Field updatedField = updatedFarm.getFields()[0];

        assertEquals(GrowthStage.READY, updatedField.getGrowthStage(), "Growth stage should be updated to READY");
        assertEquals(FieldStatus.READY, updatedField.getStatus(), "Status should be updated to READY");
    }


    @Test
    void shouldFailNotThresholdIsNotReached() {
        Seed barleySeed = new Seed();
        barleySeed.setSeedType(SeedType.BARLEY);
        barleySeed.setDaysToYoung(2);
        barleySeed.setDaysToMature(5);
        barleySeed.setDaysToReady(10);
        seedRepository.save(barleySeed);

        Field field = new Field();
        field.setSeedType(SeedType.BARLEY);
        field.setStatus(FieldStatus.PLANTED);
        field.setGrowthStage(GrowthStage.SEEDLING);

        field.setPlantedDate(Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        Farm farm = new Farm();
        farm.setId("farm-2");
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        scheduler.updateGrowthStages();

        Farm savedFarm = farmRepository.findById("farm-2").orElseThrow();
        assertEquals(GrowthStage.SEEDLING, savedFarm.getFields()[0].getGrowthStage(),
                "Should still be a SEEDLING after only 1 day");
    }


    @Test
    void shouldFailSeedRuleIsMissing() {
        String farmId = "farm-missing-rule";
        Field field = new Field();
        field.setId(1);
        field.setSeedType(SeedType.CORN);
        field.setStatus(FieldStatus.PLANTED);
        field.setGrowthStage(GrowthStage.SEEDLING);

        field.setPlantedDate(Date.from(LocalDate.now().minusDays(100)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        scheduler.updateGrowthStages();

        Farm updatedFarm = farmRepository.findById(farmId).orElseThrow();
        Field updatedField = updatedFarm.getFields()[0];

        assertEquals(GrowthStage.SEEDLING, updatedField.getGrowthStage(),
                "Growth stage should remain SEEDLING when seed rule is missing");

        assertEquals(FieldStatus.PLANTED, updatedField.getStatus(),
                "Status should remain PLANTED when seed rule is missing");
    }


    @Test
    void shouldUpdateStageSuccessfully() {
        farmRepository.deleteAll();
        seedRepository.deleteAll();

        Seed cornSeed = new Seed();
        cornSeed.setSeedType(SeedType.CORN);
        cornSeed.setDaysToYoung(1);
        cornSeed.setDaysToMature(2);
        cornSeed.setDaysToReady(5);
        seedRepository.save(cornSeed);

        Field field = new Field();
        field.setId(1);
        field.setSeedType(SeedType.CORN);
        field.setStatus(FieldStatus.PLANTED);
        field.setGrowthStage(GrowthStage.SEEDLING);

        long sixDaysInMillis = 6L * 24L * 60L * 60L * 1000L;
        field.setPlantedDate(new Date(System.currentTimeMillis() - sixDaysInMillis));

        Farm farm = new Farm();
        farm.setId("farm-1");
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        scheduler.updateGrowthStages();

        Farm updatedFarm = farmRepository.findById("farm-1").orElseThrow();
        Field updatedField = updatedFarm.getFields()[0];

        // Now (6 >= 5) is TRUE
        assertEquals(GrowthStage.READY, updatedField.getGrowthStage());
        assertEquals(FieldStatus.READY, updatedField.getStatus());
    }


    @Test
    void shouldUpdateToYoungStageSuccessfully() {
        farmRepository.deleteAll();
        seedRepository.deleteAll();

        Seed cornSeed = new Seed();
        cornSeed.setSeedType(SeedType.CORN);
        cornSeed.setDaysToYoung(1);
        cornSeed.setDaysToMature(2);
        cornSeed.setDaysToReady(5);
        seedRepository.save(cornSeed);

        Field field = new Field();
        field.setSeedType(SeedType.CORN);
        field.setStatus(FieldStatus.PLANTED);
        field.setGrowthStage(GrowthStage.SEEDLING);

        long thirtySixHours = 36L * 60L * 60L * 1000L;
        field.setPlantedDate(new Date(System.currentTimeMillis() - thirtySixHours));

        Farm farm = new Farm();
        farm.setId("farm-young");
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        scheduler.updateGrowthStages();

        Field updated = farmRepository.findById("farm-young").get().getFields()[0];
        assertEquals(GrowthStage.YOUNG, updated.getGrowthStage());
    }
    @Test
    void shouldUpdateToMatureStageSuccessfully() {
        farmRepository.deleteAll();
        seedRepository.deleteAll();

        Seed cornSeed = new Seed();
        cornSeed.setSeedType(SeedType.CORN);
        cornSeed.setDaysToYoung(1);
        cornSeed.setDaysToMature(2);
        cornSeed.setDaysToReady(5);
        seedRepository.save(cornSeed);

        Field field = new Field();
        field.setSeedType(SeedType.CORN);
        field.setStatus(FieldStatus.PLANTED);
        field.setGrowthStage(GrowthStage.SEEDLING);

        long threeDays = 3L * 24L * 60L * 60L * 1000L;
        field.setPlantedDate(new Date(System.currentTimeMillis() - threeDays));

        Farm farm = new Farm();
        farm.setId("farm-mature");
        farm.setFields(new Field[]{field});
        farmRepository.save(farm);

        scheduler.updateGrowthStages();

        Field updated = farmRepository.findById("farm-mature").get().getFields()[0];
        assertEquals(GrowthStage.MATURE, updated.getGrowthStage());
    }



    @Test
    void ShouldTestCalculateMinutesElapsedSuccessfully() {
        Date fiveMinsAgo = new Date(System.currentTimeMillis() - (5 * 60 * 1000));

        long result = ReflectionTestUtils.invokeMethod(scheduler, "calculateMinutesElapsed", fiveMinsAgo);

        assertEquals(5, result);
    }


}