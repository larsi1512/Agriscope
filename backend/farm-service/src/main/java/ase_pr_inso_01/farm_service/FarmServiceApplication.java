package ase_pr_inso_01.farm_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FarmServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarmServiceApplication.class, args);
	}

}
