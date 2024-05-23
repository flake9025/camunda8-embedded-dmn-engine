package com.github.flake9025.camunda8.config;

import io.camunda.zeebe.dmn.DecisionEngine;
import io.camunda.zeebe.dmn.impl.DmnScalaDecisionEngine;
import lombok.extern.slf4j.Slf4j;
import org.camunda.dmn.DmnEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CamundaConfig {

    @Bean
    public DecisionEngine decisionEngine() {
        log.debug("Initialisation du moteur Camunda (FEEL Decision Engine)");
        return  new DmnScalaDecisionEngine();
    }
}
