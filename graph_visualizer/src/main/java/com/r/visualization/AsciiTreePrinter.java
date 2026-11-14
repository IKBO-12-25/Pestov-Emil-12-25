package com.r.visualization;

import com.r.graph.DependencyGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AsciiTreePrinter {
    /** Строит ASCII-дерево и помечает повторные узлы и циклические рёбра. */
    public String print(DependencyGraph graph) {
        if (graph.adjacencyList().isEmpty()) {
            return "";
        }
        String root = findRoot(graph);
        StringBuilder result = new StringBuilder(root).append('\n');
        Set<String> visited = new HashSet<>();
        visited.add(root);
        appendChildren(graph, root, "", new HashSet<>(Set.of(root)), visited, result);
        return result.toString();
    }

    private String findRoot(DependencyGraph graph) {
        Set<String> dependencies = new HashSet<>();
        graph.adjacencyList().values().forEach(dependencies::addAll);
        return graph.adjacencyList().keySet().stream()
                .filter(name -> !dependencies.contains(name))
                .findFirst()
                .orElseGet(() -> graph.adjacencyList().keySet().iterator().next());
    }

    private void appendChildren(
            DependencyGraph graph,
            String packageName,
            String prefix,
            Set<String> currentPath,
            Set<String> visited,
            StringBuilder result
    ) {
        List<String> dependencies = graph.adjacencyList().getOrDefault(packageName, List.of());
        for (int index = 0; index < dependencies.size(); index++) {
            String dependency = dependencies.get(index);
            boolean last = index == dependencies.size() - 1;
            String branch = last ? "└── " : "├── ";
            result.append(prefix).append(branch).append(dependency);

            if (currentPath.contains(dependency)) {
                result.append(" [цикл]").append('\n');
            } else if (!visited.add(dependency)) {
                result.append(" [уже показан]").append('\n');
            } else {
                result.append('\n');
                Set<String> nextPath = new HashSet<>(currentPath);
                nextPath.add(dependency);
                String nextPrefix = prefix + (last ? "    " : "│   ");
                appendChildren(graph, dependency, nextPrefix, nextPath, visited, result);
            }
        }
    }
}
