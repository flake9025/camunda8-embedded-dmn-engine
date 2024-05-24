package com.github.flake9025.camunda8.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonTypeDetector {

    private final ObjectMapper objectMapper;

    /**
     * Renvoie une instance de la valeur java correspondant au flux JSON
     * @param json
     * @return object instance
     * @throws IOException
     */
    public Object parseJson(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        if (jsonNode.isBoolean()) {
            return jsonNode.booleanValue();
        } else if (jsonNode.isInt()) {
            return jsonNode.intValue();
        } else if (jsonNode.isDouble()) {
            return jsonNode.doubleValue();
        } else if (jsonNode.isTextual()) {
            return jsonNode.textValue();
        } else if (jsonNode.isObject()) {
            return objectMapper.convertValue(jsonNode, Map.class);
        } else if (jsonNode.isArray()) {
            return objectMapper.convertValue(jsonNode, List.class);
        } else {
            throw new IllegalArgumentException("Unsupported JSON type");
        }
    }
}
