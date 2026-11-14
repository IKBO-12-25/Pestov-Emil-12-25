package com.r.visualization;

import com.r.graph.DependencyGraph;

import java.util.Map;

public class PlantUmlGenerator {
    /** Создаёт текст диаграммы PlantUML из списка смежности графа. */
    public String generate(DependencyGraph graph) {
        StringBuilder result = new StringBuilder();
        result.append("@startuml\n")
                .append("left to right direction\n")
                .append("skinparam packageStyle rectangle\n")
                .append("skinparam shadowing false\n");

        if (graph.adjacencyList().values().stream().allMatch(java.util.List::isEmpty)) {
            graph.adjacencyList().keySet().forEach(name ->
                    result.append('"').append(escape(name)).append('"').append('\n'));
        } else {
            for (Map.Entry<String, java.util.List<String>> entry
                    : graph.adjacencyList().entrySet()) {
                for (String dependency : entry.getValue()) {
                    result.append('"').append(escape(entry.getKey())).append('"')
                            .append(" --> ")
                            .append('"').append(escape(dependency)).append('"')
                            .append('\n');
                }
            }
        }
        return result.append("@enduml\n").toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
