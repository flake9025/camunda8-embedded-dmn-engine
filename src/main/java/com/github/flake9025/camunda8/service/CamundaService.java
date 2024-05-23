package com.github.flake9025.camunda8.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class CamundaService {

    private final DecisionEngine decisionEngine;

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

    public List<String> getDmnExpressions(ParsedDmnScalaDrg decisionGraph) {
        log.debug("Parsing des expressions du DMN : {} ", decisionGraph.getName());
        return asJavaStream(decisionGraph.getParsedDmn().decisions())
                .flatMap(d -> getDecisionExpressions(d).stream())
                .toList();
    }

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

    private static <T> Stream<T> asJavaStream(scala.collection.Iterable<T> scalaIterable) {
        return asJavaStream(scalaIterable.iterator());
    }

    private static <T> Stream<T> asJavaStream(scala.collection.Iterator<T> scalaIterator) {
        java.util.Iterator<T> javaIterator = JavaConverters.asJavaIterator(scalaIterator);
        Iterable<T> iterable = () -> javaIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
