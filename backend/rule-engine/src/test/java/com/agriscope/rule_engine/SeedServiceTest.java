package com.agriscope.rule_engine;

import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.model.Seed;
import com.agriscope.rule_engine.repository.SeedRepository;
import com.agriscope.rule_engine.service.SeedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedServiceTest {

    @Mock
    private SeedRepository seedRepository;

    @InjectMocks
    private SeedService seedService;

    @Test
    void getSeed_ShouldReturnSeed_WhenSeedExists() {
        SeedType type = SeedType.WHEAT;
        Seed originalSeed = new Seed();
        originalSeed.setSeedType(type);

        when(seedRepository.findBySeedType(type)).thenReturn(Optional.of(originalSeed));
        Seed result = seedService.getSeed(type);

        assertNotNull(result);
        assertEquals(type, result.getSeedType());
        verify(seedRepository, times(1)).findBySeedType(type);
    }

    @Test
    void getSeed_ShouldReturnNull_WhenSeedDoesNotExist() {
        SeedType type = SeedType.CORN;
        when(seedRepository.findBySeedType(type)).thenReturn(Optional.empty());

        Seed result = seedService.getSeed(type);

        assertNull(result);
        verify(seedRepository, times(1)).findBySeedType(type);
    }
}