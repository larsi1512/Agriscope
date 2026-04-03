package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.SeedDto;
import ase_pr_inso_01.farm_service.models.Seed;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
import ase_pr_inso_01.farm_service.repository.SeedRepository;
import ase_pr_inso_01.farm_service.service.impl.SeedServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedServiceImplGetSeedsTests {

    @Mock
    private SeedRepository seedRepository;

    @InjectMocks
    private SeedServiceImpl seedService;

    @Test
    void ShouldGetAllSeeds() {
        Seed seed = createTestSeed("1", SeedType.CORN, "Corn", "Zea mays");
        when(seedRepository.findAll()).thenReturn(List.of(seed));

        List<SeedDto> result = seedService.getAllSeeds();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());

        SeedDto dto = result.getFirst();
        assertEquals("1", dto.getId());
        assertEquals(SeedType.CORN, dto.getSeedType());
        assertEquals("Corn", dto.getDisplayName());
        assertEquals("Zea mays", dto.getScientificName());
        assertEquals(10.0, dto.getMinTemperature());
        assertEquals(1.5, dto.getSeedCoefficient());
    }

    @Test
    void ShouldGetAllSeedsWhenEmpty() {
        when(seedRepository.findAll()).thenReturn(List.of());
        List<SeedDto> result = seedService.getAllSeeds();
        assertTrue(result.isEmpty());
    }

    private Seed createTestSeed(String id, SeedType type, String display, String scientific) {
        Seed seed = new Seed();
        seed.setId(id);
        seed.setSeedType(type);
        seed.setDisplayName(display);
        seed.setScientificName(scientific);
        seed.setMinTemperature(10.0);
        seed.setMaxTemperature(35.0);
        seed.setSeedCoefficient(1.5);
        return seed;
    }


    @Test
    void ShouldGetByDisplayNameSuccessfully() {
        SeedDto result = seedService.getByDisplayName("Corn");
        assertNotNull(result);
    }

    @Test
    void getSeedByName_ShouldReturnNull_WhenNotYetImplemented() {
        String seedName = "Corn";
        Seed result = seedService.getSeedByName(seedName);
        assertNull(result, "The method is currently expected to return null");
    }
}