package com.r.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyLoadOrderCalculator {
    /**
     * Возвращает порядок загрузки, помещая каждую зависимость перед использующим её пакетом.
     */
    public DependencyLoadOrder calculate(DependencyGraph graph) {
        if (!graph.cycles().isEmpty()) {
            return new DependencyLoadOrder(List.of(), graph.cycles());
        }

        Set<String> visited = new HashSet<>();
        List<String> order = new ArrayList<>();
        for (String packageName : graph.adjacencyList().keySet()) {
            visit(packageName, graph, visited, order);
        }
        return new DependencyLoadOrder(order, List.of());
    }

    private void visit(
            String packageName,
            DependencyGraph graph,
            Set<String> visited,
            List<String> order
    ) {
        if (!visited.add(packageName)) {
            return;
        }
        for (String dependency : graph.adjacencyList().getOrDefault(packageName, List.of())) {
            visit(dependency, graph, visited, order);
        }
        order.add(packageName);
    }
}
