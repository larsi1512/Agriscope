
package com.agriscope.rule_engine.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DroolsConfig {

    private static final String RULES_SAFETY_PATH = "rules/safety/";
    private static final String RULES_IRRIGATION_PATH = "rules/irrigation/";
    private static final String RULES_DISEASE_PATH = "rules/disease/";
    private KieServices kieServices = KieServices.Factory.get();

    @Bean
    public KieContainer kieContainer() {

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_SAFETY_PATH + "wheat.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_SAFETY_PATH + "corn.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_SAFETY_PATH + "barley.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_SAFETY_PATH + "pumpkin.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_SAFETY_PATH + "black_grapes.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_SAFETY_PATH + "white_grapes.drl"));

        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "irrigation_common.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "wheat.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "corn.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "barley.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "pumpkin.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "black_grapes.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_IRRIGATION_PATH + "white_grapes.drl"));

        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_DISEASE_PATH + "wheat.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_DISEASE_PATH + "corn.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_DISEASE_PATH + "barley.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_DISEASE_PATH + "pumpkin.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_DISEASE_PATH + "black_grapes.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_DISEASE_PATH + "white_grapes.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();


        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            kieBuilder.getResults().getMessages(Message.Level.ERROR).forEach(msg ->
                    log.error("  - {}", msg.getText())
            );
            throw new RuntimeException("Drools build failed!");
        }

        KieModule kieModule = kieBuilder.getKieModule();
        log.info("Drools rules loaded successfully");

        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    @Bean
    public KieSession kieSession() {
        return kieContainer().newKieSession();
    }
}