package ase_pr_inso_01.farm_service.service;


import ase_pr_inso_01.farm_service.controller.dto.farm.*;
import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import ase_pr_inso_01.farm_service.models.enums.SoilType;
import ase_pr_inso_01.farm_service.repository.FarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class FarmServiceImplGetFarmsTests {


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
    void shouldGetFarmsByUserEmails() throws Exception {
        Farm f1 = new Farm();
        f1.setName("Farm A");
        f1.setUserId(TEST_USER_ID);
        f1.setLatitude(48.0f);
        f1.setLongitude(16.0f);
        f1.setSoilType(SoilType.LOAM);
        f1.setFields(new Field[0]);

        Farm f2 = new Farm();
        f2.setName("Farm B");
        f2.setUserId(TEST_USER_ID);
        f2.setLatitude(49.0f);
        f2.setLongitude(17.0f);
        f2.setSoilType(SoilType.CLAY);
        f2.setFields(new Field[0]);

        farmRepository.saveAll(java.util.List.of(f1, f2));

        var result = farmService.getFarmsByUserEmail(TEST_EMAIL);
        assertEquals(2, result.size());
    }


    @Test
    void shouldReturnEmptyCheckUserHasFarms() throws Exception {
        UserDetailsDto user = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(user);

        FarmCheckDto result = farmService.checkUserHasFarms(TEST_EMAIL);

        assertFalse(result.isHasFarms());
        assertEquals(0, result.getFarmCount());
    }

    @Test
    void ShouldCheckUserHasFarmsSuccessfully() throws Exception {
        UserDetailsDto user = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(contains("/api/users/by-email/"), eq(UserDetailsDto.class)))
                .thenReturn(user);

        Farm f1 = createValidFarm("Sunshine Acres", TEST_USER_ID);
        Farm f2 = createValidFarm("Moonlight Fields", TEST_USER_ID);
        farmRepository.save(f1);
        farmRepository.save(f2);

        FarmCheckDto result = farmService.checkUserHasFarms(TEST_EMAIL);

        assertTrue(result.isHasFarms(), "Should indicate that user has farms");
        assertEquals(2, result.getFarmCount(), "Should count exactly 2 farms");
    }


    @Test
    void ShouldSaveAndReturnFarmFromDatabase() throws Exception {
        String farmId = "farm-123";

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setName("Farm1");
        farm.setUserId(TEST_USER_ID);
        farm.setLatitude(47.0f);
        farm.setLongitude(13.0f);
        farm.setFields(new Field[0]);

        farmRepository.save(farm);

        UserDetailsDto userDto = new UserDetailsDto(TEST_USER_ID, TEST_EMAIL);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(userDto);

        FarmDetailsDto result = farmService.getFarmById(farmId, TEST_EMAIL);

        assertNotNull(result);
        assertEquals(farmId, result.id());
        assertEquals(TEST_USER_ID, farm.getUserId());
    }



    @Test
    void ShouldFailGetByIdSomeoneElsesFarm() throws Exception {
        String farmId = "stranger-farm-999";
        String actualOwnerId = "user-owner";
        String hackerUserId = "user-hacker";
        String hackerEmail = "hacker@example.com";

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setName("Private Property");
        farm.setUserId(actualOwnerId); // Farm belongs to USER A
        farm.setLatitude(47.0f);
        farm.setLongitude(13.0f);
        farm.setFields(new Field[0]);

        farmRepository.save(farm);

        UserDetailsDto hackerDto = new UserDetailsDto(hackerUserId, hackerEmail);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(hackerDto);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.getFarmById(farmId, hackerEmail);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"),
                "Expected unauthorized message but got: " + exception.getMessage());
    }


    @Test
    void ShouldFailUpdateFarmFarmDoesNotExist() {
        String nonExistentFarmId = "non-existent-id";
        String userEmail = "test@example.com";
        UserDetailsDto userDto = new UserDetailsDto(TEST_USER_ID, userEmail);
        when(restTemplate.getForObject(anyString(), eq(UserDetailsDto.class)))
                .thenReturn(userDto);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            farmService.getFarmById(nonExistentFarmId, userEmail);
        });

        assertEquals("Farm not found with ID: " + nonExistentFarmId, exception.getMessage());
    }
    private Farm createValidFarm(String name, String userId) {
        Farm farm = new Farm();
        farm.setName(name);
        farm.setUserId(userId);
        farm.setLatitude(47.0f);
        farm.setLongitude(13.0f);
        farm.setSoilType(SoilType.SILT);
        farm.setFields(new Field[0]);
        return farm;
    }


}
