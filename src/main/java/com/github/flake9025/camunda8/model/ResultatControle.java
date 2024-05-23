package com.github.flake9025.camunda8.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class ResultatControle {

    private String codeControle;
    private Map<String, Object> variablesEntree;
    private Map<String, Object> variablesSortie;
}
