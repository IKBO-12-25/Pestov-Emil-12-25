package com.r.visualization;

import com.r.graph.DependencyGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUmlGeneratorTest {
    @Test
    void createsPlantUmlRelationships() {
        DependencyGraph graph = new DependencyGraph(
                Map.of("A", List.of("B"), "B", List.of()), List.of()
        );

        String result = new PlantUmlGenerator().generate(graph);

        assertTrue(result.contains("@startuml"));
        assertTrue(result.contains("\"A\" --> \"B\""));
        assertTrue(result.contains("@enduml"));
    }
}
