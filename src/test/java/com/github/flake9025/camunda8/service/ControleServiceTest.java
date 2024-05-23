package com.github.flake9025.camunda8.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.flake9025.camunda8.TestApplication;
import com.github.flake9025.camunda8.helper.DMNLoader;
import com.github.flake9025.camunda8.model.ResultatControle;
import io.camunda.zeebe.dmn.impl.ParsedDmnScalaDrg;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApplication.class)
class ControleServiceTest {

    @Autowired
    private DMNLoader dmnLoader;
    @Autowired
    private ControleService controleService;

    protected ParsedDmnScalaDrg getControleFromResourcesByCode(String code) throws IOException {
        return dmnLoader.chargerDMNs().getOrDefault(code, null);
    }

    @Test
    void jouerControle_Dish() throws IOException {
        ParsedDmnScalaDrg controle = getControleFromResourcesByCode("Dish");
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("season", "Fall");
        List<ResultatControle> results = controleService.jouerControle(controle, donnees);
        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(results.get(0).getVariablesSortie().get("Dish")).isEqualTo("Spareribs");
    }
}
