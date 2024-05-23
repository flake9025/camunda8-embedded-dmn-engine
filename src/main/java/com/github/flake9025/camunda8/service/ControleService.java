package com.github.flake9025.camunda8.service;

import com.github.flake9025.camunda8.model.ResultatControle;
import io.camunda.zeebe.dmn.DecisionEngine;
import io.camunda.zeebe.dmn.DecisionEvaluationResult;
import io.camunda.zeebe.dmn.impl.ParsedDmnScalaDrg;
import io.camunda.zeebe.dmn.impl.VariablesContext;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.apache.commons.collections4.CollectionUtils;
import org.camunda.dmn.parser.ParsedDecision;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ControleService {

    private final CamundaService camundaService;
    private final DecisionEngine decisionEngine;

    /**
     * Controler un flux pour un DMN spécifique
     *
     * @param controle
     * @param dmnVariables
     * @return
     */
    public List<ResultatControle> jouerControle(ParsedDmnScalaDrg controle, Map<String, Object> dmnVariables) {
        List<ResultatControle> resultats = new ArrayList<>();
        CamundaService.asJavaStream(controle.getParsedDmn().decisions()).forEach(dmnDecision -> {
            String codeControle = dmnDecision.name();
            log.info("Play DMN ... {}", codeControle);

            // on filtre les variables d'entrée vraiment utilisées par la table de décision
            Map<String, Object> camundaVariables = buildDecisionInputsAndValues(dmnDecision, dmnVariables);
            VariablesContext variablesContext = new VariablesContext(camundaVariables);

            // Execution de la table de décision (avec les variables réduites)
            DecisionEvaluationResult dmnDecisionResult = decisionEngine.evaluateDecisionById(controle, dmnDecision.id(), variablesContext);

            // Récupération des variables de sortie
            Map<String, Object> dmnOutputVariables = new HashMap<>();
            if(!dmnDecisionResult.isFailure()) {
                CollectionUtils.emptyIfNull(dmnDecisionResult.getEvaluatedDecisions()).forEach(evaluatedDecision -> {
                    String key = evaluatedDecision.decisionName();
                    DirectBuffer buffer = evaluatedDecision.decisionOutput();
                    String value = MsgPackConverter.convertToJson(buffer).replace("\"", "");
                    log.info("result : [{}, {}]", key, value);
                    dmnOutputVariables.put(key, value);
                });
            } else {
                log.error(dmnDecisionResult.getFailureMessage());
            }

            // Préparation d'un bilan de résultats
            resultats.add(new ResultatControle()
                    .setCodeControle(codeControle)
                    .setVariablesEntree(camundaVariables)
                    .setVariablesSortie(dmnOutputVariables)
            );
        });
        return resultats;
    }

    private Map<String, Object> buildDecisionInputsAndValues(ParsedDecision decision, Map<String, Object> dmnVariables) {
        List<String> expressions = camundaService.getDecisionExpressions(decision);
        Map<String, Object> variablesValues = new HashMap<>();
        if(CollectionUtils.isNotEmpty(expressions)) {
            List<String> variablesKeys = camundaService.getDmnVariablesKeys(decision.name(), expressions, dmnVariables.keySet());
            variablesKeys.forEach(key -> variablesValues.put(key, dmnVariables.get(key)));
        }
        return variablesValues;
    }
}
