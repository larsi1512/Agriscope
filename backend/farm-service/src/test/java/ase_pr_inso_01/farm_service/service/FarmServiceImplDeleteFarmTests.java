package ase_pr_inso_01.farm_service.service;


import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import ase_pr_inso_01.farm_service.controller.dto.farm.UserDetailsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
public class FarmServiceImplDeleteFarmTests {
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
    void ShouldDeleteFarmSuccessfully() throws Exception {
        String farmId = "delete-me-123";
        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setUserId(TEST_USER_ID);
        farmRepository.save(farm); // Save to real DB

        UserDetailsDto userDto = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(userDto);

        farmService.deleteFarm(farmId, TEST_EMAIL);

        Optional<Farm> deletedFarm = farmRepository.findById(farmId);
        assertTrue(deletedFarm.isEmpty(), "Farm should have been deleted from the database");
    }

    @Test
    void ShouldFailDeleteFarmNonExistentFarm() {
        String farmId = "ghost-farm";
        UserDetailsDto userDto = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(userDto);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.deleteFarm(farmId, TEST_EMAIL);
        });

        assertEquals("Farm not found with ID: " + farmId, exception.getMessage());
    }

    @Test
    void ShouldFailDeleteFarmUnauthorizedUser() {
        String farmId = "private-farm";
        String ownerId = "real-owner";
        String hackerId = "hacker-user";

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setUserId(ownerId);
        farmRepository.save(farm);

        UserDetailsDto hackerDto = new UserDetailsDto(hackerId, "hacker@email.com");
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(hackerDto);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.deleteFarm(farmId, "hacker@email.com");
        });

        assertEquals("Unauthorized: This farm does not belong to you", exception.getMessage());

        assertTrue(farmRepository.existsById(farmId));
    }


}
