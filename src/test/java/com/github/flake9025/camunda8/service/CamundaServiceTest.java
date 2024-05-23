package com.github.flake9025.camunda8.service;

import com.github.flake9025.camunda8.TestApplication;
import io.camunda.zeebe.dmn.impl.ParsedDmnScalaDrg;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApplication.class)
class CamundaServiceTest {

    @Autowired
    private CamundaService camundaService;

    @Test
    void getDmnDecisions_shouldGetDish_Decision() throws IOException {
        ParsedDmnScalaDrg decisionGraph = getDmnDecisionGraph("Dish");
        Assertions.assertThat(decisionGraph.getDecisions().iterator().next().getName()).isEqualTo("Dish");
    }

    @Test
    void getDmnExpressions_shouldGetDish_Expression() throws IOException {
        ParsedDmnScalaDrg decisionGraph = getDmnDecisionGraph("Dish");
        List<String> expressions = camundaService.getDecisionExpressions( decisionGraph.getParsedDmn().decisions().iterator().next());
        Assertions.assertThat(expressions)
                .hasSize(9)
                .containsExactly("season", "\"Fall\"", "\"Spareribs\"", "\"Winter\"", "\"Roastbeef\"", "\"Spring\"", "\"Steak\"", "\"Summer\"", "\"Light Salad and a nice Steak\"");
    }
    @Test
    void getDmnVariablesKeys_shouldGetDish_Keys() throws IOException {
        ParsedDmnScalaDrg decisionGraph = getDmnDecisionGraph("Dish");
        List<String> expressions = camundaService.getDecisionExpressions( decisionGraph.getParsedDmn().decisions().iterator().next());
        Set<String> userVariableKeys = Set.of("test", "season", "guests", "dish", "people", "weather");
        List<String> dmnUsedKeys = camundaService.getDmnVariablesKeys("Dish", expressions, userVariableKeys);
        Assertions.assertThat(dmnUsedKeys)
                .hasSize(1)
                .containsExactly("season");
    }

    private ParsedDmnScalaDrg getDmnDecisionGraph(String key) throws IOException {
        ClassPathResource resource = new ClassPathResource("/dmn/" + key + ".dmn");
        return camundaService.getDmnDecisionGraph(key, resource.getInputStream());
    }
}
