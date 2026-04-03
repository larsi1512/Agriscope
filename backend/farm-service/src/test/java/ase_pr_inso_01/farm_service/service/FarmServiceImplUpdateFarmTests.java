package ase_pr_inso_01.farm_service.service;



import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.enums.FieldStatus;
import ase_pr_inso_01.farm_service.models.enums.GrowthStage;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
import ase_pr_inso_01.farm_service.models.enums.SoilType;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class FarmServiceImplUpdateFarmTests {


    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmService farmService;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private final String TEST_EMAIL = "farmer@test.com";
    private final String TEST_USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        farmRepository.deleteAll();
        UserDetailsDto mockUser = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(mockUser);

    }

    @Test
    void shouldUpdateField() throws Exception {

        Field field = new Field();
        field.setId(1);
        field.setStatus(FieldStatus.EMPTY);
        field.setSeedType(SeedType.CORN);
        field.setGrowthStage(GrowthStage.MATURE);
        Farm farm = createValidFarm("Field Test Farm", TEST_USER_ID);
        farm.setFields(new Field[]{field});
        farm = farmRepository.save(farm);


        FieldDetailsDto updateDto = new FieldDetailsDto(
                1,
                FieldStatus.PLANTED,
                SeedType.BARLEY,
                null,
                null,
                GrowthStage.MATURE
        );

        FarmDetailsDto result = farmService.updateField(farm.getId(), updateDto, TEST_EMAIL);

        assertEquals(SeedType.BARLEY, result.fields()[0].seedType());
        assertEquals(FieldStatus.PLANTED, result.fields()[0].status());

        Mockito.verify(rabbitTemplate, times(1))
                .convertAndSend(eq("farm_events"), eq("farm.updated"), any(java.util.Map.class));
    }

    @Test
    void shouldFailUpdateFieldNotUsersFarm() throws Exception {
        Field field = new Field();
        field.setId(1);
        Farm farm = createValidFarm("Secret Farm", TEST_USER_ID);
        farm.setFields(new Field[]{field});
        farm = farmRepository.save(farm);

        String hackerEmail = "hacker@example.com";
        UserDetailsDto hackerUser = new UserDetailsDto("HACKER_ID", hackerEmail);
        when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(hackerUser);

        FieldDetailsDto updateDto = new FieldDetailsDto(1, FieldStatus.PLANTED, SeedType.BARLEY, null, null, GrowthStage.MATURE);
        Farm finalFarm = farm;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.updateField(finalFarm.getId(), updateDto, hackerEmail);
        });

        assertEquals("Unauthorized: This farm does not belong to you", exception.getMessage());

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void shouldFailUpdateFieldFarmDoesNotExist() {
        int nonExistentFarmId = 999;

        FieldDetailsDto updateDto = new FieldDetailsDto(
                1,
                FieldStatus.PLANTED,
                SeedType.BARLEY,
                null,
                null,
                GrowthStage.MATURE
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.updateField(String.valueOf(nonExistentFarmId), updateDto, TEST_EMAIL);
        });

        assertEquals("Farm not found with ID: " + nonExistentFarmId, exception.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(java.util.Map.class));
    }
    private Farm createValidFarm(String name, String userId) {
        Farm farm = new Farm();
        farm.setName(name);
        farm.setUserId(userId);
        farm.setLatitude(47.0f);
        farm.setLongitude(13.0f);
        farm.setSoilType(SoilType.SILT);
        farm.setFields(new Field[0]); // Crucial to avoid NPE in streams/mappers
        return farm;
    }
}
