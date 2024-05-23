package com.github.flake9025.camunda8.helper;

import com.github.flake9025.camunda8.service.CamundaService;
import io.camunda.zeebe.dmn.impl.ParsedDmnScalaDrg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class DMNLoader {

    @Autowired
    private CamundaService camundaService;

    public Map<String, ParsedDmnScalaDrg> chargerDMNs() throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        Resource[] resources = resolver.getResources("classpath*:/dmn/*.dmn");

        Map<String, ParsedDmnScalaDrg> controleList = new HashMap<>();
        for (Resource resource : resources) {
            String codeControle = Objects.requireNonNull(resource.getFilename()).split("\\.dmn")[0];
            controleList.put(codeControle, camundaService.getDmnDecisionGraph(codeControle, resource.getInputStream()));
        }
        return controleList;
    }
}
