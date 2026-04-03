package com.agriscope.rule_engine.repository;

import com.agriscope.rule_engine.domain.enums.SeedType;
import com.agriscope.rule_engine.domain.model.Seed;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SeedRepository extends MongoRepository<Seed, String> {
    Optional<Seed> findBySeedType(SeedType seedType);
}