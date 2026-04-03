package ase_pr_inso_01.farm_service.service;

import ase_pr_inso_01.farm_service.controller.dto.farm.SeedDto;
import ase_pr_inso_01.farm_service.models.Seed;

import java.util.List;

public interface SeedService {
    List<SeedDto> getAllSeeds();

    Seed getSeedByName( String name);
    SeedDto getByDisplayName(String displayName);

}
