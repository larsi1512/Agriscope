package ase_pr_inso_01.farm_service.repository;

import ase_pr_inso_01.farm_service.models.Farm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FarmRepository extends MongoRepository<Farm, String> {
    boolean existsByName(String name);

    boolean existsByNameAndUserId(String name, String userId);

    List<Farm> findByUserId(String userId);
}
