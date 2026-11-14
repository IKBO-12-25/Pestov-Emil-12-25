package com.r.graph;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyLoadOrderCalculatorTest {
    private final DependencyLoadOrderCalculator calculator =
            new DependencyLoadOrderCalculator();

    @Test
    void loadsDependenciesBeforePackagesThatNeedThem() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("A", List.of("B", "C"));
        values.put("B", List.of("D"));
        values.put("C", List.of("D"));
        values.put("D", List.of());
        DependencyGraph graph = graph(values);

        DependencyLoadOrder result = calculator.calculate(graph);

        assertTrue(result.isPossible());
        assertEquals(List.of("D", "B", "C", "A"), result.packages());
    }

    @Test
    void returnsOnlyRootForPackageWithoutDependencies() {
        DependencyLoadOrder result = calculator.calculate(
                graph(Map.of("A", List.of()))
        );

        assertEquals(List.of("A"), result.packages());
        assertTrue(result.isPossible());
    }

    @Test
    void refusesToCreateOrderForCyclicGraph() {
        DependencyGraph graph = new DependencyGraph(
                Map.of("A", List.of("B"), "B", List.of("A")),
                List.of("A -> B -> A")
        );

        DependencyLoadOrder result = calculator.calculate(graph);

        assertFalse(result.isPossible());
        assertTrue(result.packages().isEmpty());
        assertEquals(List.of("A -> B -> A"), result.cycles());
    }

    private DependencyGraph graph(Map<String, List<String>> values) {
        return new DependencyGraph(new LinkedHashMap<>(values), List.of());
    }
}
