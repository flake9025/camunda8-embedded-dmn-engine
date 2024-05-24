package com.github.flake9025.camunda8.service;

import com.github.flake9025.camunda8.util.JsonTypeDetector;
import io.camunda.zeebe.dmn.DecisionEngine;
import io.camunda.zeebe.dmn.ParsedDecisionRequirementsGraph;
import io.camunda.zeebe.dmn.impl.ParsedDmnScalaDrg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.dmn.parser.*;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import scala.collection.JavaConverters;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class CamundaService {

    private final DecisionEngine decisionEngine;
    private final JsonTypeDetector jsonTypeDetector;

    /**
     * Parse un fichier DMN et renvoie son graphe
     * @param decisionKey
     * @param inputStream
     * @return
     * @throws IOException
     */
    public ParsedDmnScalaDrg getDmnDecisionGraph(String decisionKey, InputStream inputStream) throws IOException {
        log.info("Chargement du DMN {}...", decisionKey);
        Assert.notNull(inputStream, "Stream is null, no file or cache empty ?");

        ParsedDmnScalaDrg parsedDecision = null;
        ParsedDecisionRequirementsGraph dmnDecisionGraph = decisionEngine.parse(inputStream);
        if (dmnDecisionGraph.isValid()) {
            parsedDecision = (ParsedDmnScalaDrg) dmnDecisionGraph;
        } else {
            log.error("\t Erreur de parsing : {}", dmnDecisionGraph.getFailureMessage());
        }
        return parsedDecision;
    }

    @Deprecated(since = "getDecisionExpressions")
    /**
     * Renvoie la liste de toutes les expressions d'un DMN
     * @param decisionGraph
     * @return
     */
    public List<String> getDmnExpressions(ParsedDmnScalaDrg decisionGraph) {
        log.debug("Parsing des expressions du DMN : {} ", decisionGraph.getName());
        return asJavaStream(decisionGraph.getParsedDmn().decisions())
                .flatMap(d -> getDecisionExpressions(d).stream())
                .toList();
    }

    /**
     * Renvoie la liste des expressions d'une table de décision d'un DMN
     * @param decision
     * @return
     */
    public List<String> getDecisionExpressions(ParsedDecision decision) {
        log.debug("Parsing des expressions de la décision : {} ", decision.name());
        List<String> allExpressions = null;

        // Read all table cells
        ParsedDecisionLogic decisionLogic = decision.logic();
        switch (decisionLogic) {
            case ParsedDecisionTable decisionTable -> {
                List<String> inputs = asJavaStream(decisionTable.inputs())
                        .map(ParsedInput::expression)
                        .map(expression -> ((FeelExpression) expression).expression().text())
                        .filter(StringUtils::isNotBlank)
                        .toList();
                List<String> rules = asJavaStream(decisionTable.rules())
                        .flatMap(rule -> Stream.concat(
                                asJavaStream(rule.inputEntries())
                                        .filter(expression -> FeelExpression.class.isAssignableFrom(expression.getClass()))
                                        .map(expression -> ((FeelExpression) expression).expression().text())
                                        .filter(StringUtils::isNotBlank),
                                asJavaStream(rule.outputEntries())
                                        .map(o -> o._2)
                                        .filter(expression -> FeelExpression.class.isAssignableFrom(expression.getClass()))
                                        .map(expression -> ((FeelExpression) expression).expression().text())
                                        .filter(StringUtils::isNotBlank)
                        ))
                        .filter(StringUtils::isNotBlank)
                        .toList();
                allExpressions = Stream.concat(inputs.stream(), rules.stream()).toList();
            }
            case ParsedLiteralExpression expression when FeelExpression.class.isAssignableFrom(expression.expression().getClass()) -> {
                allExpressions = List.of(((FeelExpression) expression.expression()).expression().text());
            }
            default -> throw new IllegalStateException("Unexpected type: " + decisionLogic.getClass().getSimpleName());
        }
        log.debug("DMN expressions : {} ", allExpressions);
        return allExpressions;
    }

    /**
     * Filtre la liste des variables passée en paramètre avec les clés vraiment utilisées par la table de decision
     * @param decisionKey
     * @param dmnExpressions
     * @param variablesKeys
     * @return
     */
    public List<String> getDmnVariablesKeys(String decisionKey, List<String> dmnExpressions, Set<String> variablesKeys) {
        log.debug("Matching DMN Dxpressions with user Variables Keys for DMN : {} ", decisionKey);
        List<String> dmnInputKeys = new ArrayList<>();
        CollectionUtils.emptyIfNull(dmnExpressions).forEach(expression -> variablesKeys.forEach(variableKey -> {
            if (expression.contains(variableKey)) {
                log.debug("Decision '{}' : expression '{}' uses key '{}'", decisionKey, expression, variableKey);
                if (!dmnInputKeys.contains(variableKey)) {
                    dmnInputKeys.add(variableKey);
                }
            }
        }));
        return dmnInputKeys;
    }

    public List<String> getDecisionOutputNames(ParsedDecision decision) {
        log.debug("Parsing des outputs de la décision : {} ", decision.name());
        List<String> outputsNames = null;

        ParsedDecisionLogic decisionLogic = decision.logic();
        switch (decisionLogic) {
            case ParsedDecisionTable decisionTable -> {
                outputsNames = asJavaStream(decisionTable.outputs())
                        .map(ParsedOutput::name)
                        .toList();
            }
            default -> throw new IllegalStateException("Unexpected type: " + decisionLogic.getClass().getSimpleName());
        }
        log.debug("DMN outputs names : {} ", outputsNames);
        return outputsNames;
    }

    /**
     * Convertit un Iterable Scala en Stream Java
     * @param scalaIterable
     * @return
     * @param <T>
     */
    public <T> Stream<T> asJavaStream(scala.collection.Iterable<T> scalaIterable) {
        return asJavaStream(scalaIterable.iterator());
    }

    /**
     * Convertit un Iterator Scala en Stream Java
     * @param scalaIterator
     * @return
     * @param <T>
     */
    public <T> Stream<T> asJavaStream(scala.collection.Iterator<T> scalaIterator) {
        java.util.Iterator<T> javaIterator = JavaConverters.asJavaIterator(scalaIterator);
        Iterable<T> iterable = () -> javaIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Renvoie une instance de map pour les resultats d'un controle DMN
     * @param decision
     * @param jsonResults
     * @return object instance
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildResults(ParsedDecision decision, String jsonResults) {
        Map<String, Object> result = null;
        List<String> outputNames = getDecisionOutputNames(decision);
        Assert.notEmpty(outputNames, "DMN must have output columns");
        try {
            Object jsonObject = jsonTypeDetector.parseJson(jsonResults);
            if (outputNames.size() == 1) {
                // build a map with output name and result
                result = new HashMap<>();
                result.put(outputNames.getFirst(), jsonObject);
            } else if (Map.class.isAssignableFrom(jsonObject.getClass())){
                // result is already a map: keep it
                result = (Map<String, Object>) jsonObject;
            } else {
                log.error("columns outputs '{}' don't match with results '{}'", outputNames, jsonResults);
                throw new IllegalStateException("columns outputs don't match with results");
            }
        } catch (IOException e) {
            log.error("results parsing error : {}", e.getMessage(), e);
            // build a map with json result
            result = Map.of(outputNames.getFirst(), jsonResults);
        }
        return result;
    }
}
