package ase_pr_inso_01.farm_service.repository;

import ase_pr_inso_01.farm_service.models.Seed;
import ase_pr_inso_01.farm_service.models.enums.SeedType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SeedRepository extends MongoRepository<Seed, String> {
    Optional<Seed> findBySeedType(SeedType seedType);
}