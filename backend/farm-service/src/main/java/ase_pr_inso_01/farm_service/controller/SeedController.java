package ase_pr_inso_01.farm_service.controller;


import ase_pr_inso_01.farm_service.controller.dto.farm.SeedDto;
import ase_pr_inso_01.farm_service.models.Seed;
import ase_pr_inso_01.farm_service.service.SeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping(value = "/api/seeds")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @GetMapping("/getAll")
    public List<SeedDto> getAllSeeds() {
        return seedService.getAllSeeds();
    }

    @GetMapping("/getByName/{name}")
    public ResponseEntity<SeedDto> getSeedByName(@PathVariable String name) {
        try {
            SeedDto seed = seedService.getByDisplayName(name);
            return ResponseEntity.ok(seed);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }    }
}
