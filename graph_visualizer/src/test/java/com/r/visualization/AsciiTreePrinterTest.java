package com.r.visualization;

import com.r.graph.DependencyGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsciiTreePrinterTest {
    @Test
    void marksSharedDependenciesAndCycles() {
        DependencyGraph graph = new DependencyGraph(
                Map.of("A", List.of("B", "C"), "B", List.of("D"),
                        "C", List.of("D"), "D", List.of()), List.of()
        );

        String result = new AsciiTreePrinter().print(graph);

        assertTrue(result.contains("A"));
        assertTrue(result.contains("[уже показан]"));
    }

    @Test
    void marksCycleInTree() {
        DependencyGraph graph = new DependencyGraph(
                Map.of("A", List.of("B"), "B", List.of("A")), List.of("A -> B -> A")
        );

        assertTrue(new AsciiTreePrinter().print(graph).contains("[цикл]"));
    }
}
