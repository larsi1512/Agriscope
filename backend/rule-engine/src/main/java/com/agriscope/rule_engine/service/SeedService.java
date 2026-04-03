package com.agriscope.rule_engine.service;

import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.model.Seed;
import com.agriscope.rule_engine.repository.SeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {

    private final SeedRepository seedRepository;


    public Seed getSeed(SeedType seedType) {
        Optional<Seed> seedOpt = seedRepository.findBySeedType(seedType);

        if (seedOpt.isEmpty()) {
            log.error("Seed not found in database for type: {}. Rules may fail.", seedType);
            return null;
        }

        return seedOpt.get().copy();
    }
}