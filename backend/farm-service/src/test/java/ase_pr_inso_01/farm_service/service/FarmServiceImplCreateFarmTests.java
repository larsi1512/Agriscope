package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.FieldCreateDto;
import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.enums.SoilType;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.controller.dto.farm.FarmCreateDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.UserDetailsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FarmServiceImplCreateFarmTests {

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
        Mockito.when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(mockUser);

    }

    @Test
    void shouldCreateFarmAndStoreInDatabase() throws Exception {
        Farm existing = new Farm();
        existing.setName("Green Valley");
        existing.setLatitude(47.6114867f);
        existing.setLongitude(13.760376f);
        existing.setSoilType(SoilType.SILT);
        existing.setUserId(TEST_USER_ID);
        existing.setFields(new Field[0]);

        farmRepository.save(existing);

        FarmCreateDto dto = new FarmCreateDto(
                "Green Valley",
                47.6114867f,
                13.760376f,
                SoilType.SILT,
                new FieldCreateDto[0]
        );

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> farmService.createFarm(dto, TEST_EMAIL)
        );

        assertEquals("You already have a farm with this name", ex.getMessage());

        assertEquals(1, farmRepository.findAll().size());

        Mockito.verify(rabbitTemplate, Mockito.never())
                .convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
    }

    @Test
    void shouldFailSameName() throws Exception {
        FarmCreateDto dto = new FarmCreateDto(
                "Green Valley",
                47.6114867f,
                13.760376f,
                SoilType.SILT,
                new FieldCreateDto[0]
        );

        FarmCreateDto dto2 = new FarmCreateDto(
                "Green Valley",
                47.6114867f,
                13.760376f,
                SoilType.CLAY,
                new FieldCreateDto[0]
        );

        // 1. First creation should work fine
        farmService.createFarm(dto, TEST_EMAIL);

        // 2. Second creation should hit the 'existsByNameAndUserId' check
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.createFarm(dto2, TEST_EMAIL);
        });

        // 3. Verify the specific error message from your service
        assertEquals("You already have a farm with this name", exception.getMessage());
    }



}
