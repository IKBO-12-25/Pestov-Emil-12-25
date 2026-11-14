package com.r.graph;

import com.r.dependency.Dependency;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyGraphBuilder {
    private final DependencyProvider dependencyProvider;
    private final Map<String, VisitState> states = new LinkedHashMap<>();
    private final Map<String, List<String>> adjacencyList = new LinkedHashMap<>();
    private final List<String> cycles = new ArrayList<>();
    private final List<String> currentPath = new ArrayList<>();

    public DependencyGraphBuilder(DependencyProvider dependencyProvider) {
        this.dependencyProvider = dependencyProvider;
    }

    /**
     * Строит полный граф зависимостей, начиная с указанного пакета.
     */
    public DependencyGraph build(String rootPackage) {
        states.clear();
        adjacencyList.clear();
        cycles.clear();
        currentPath.clear();
        visit(rootPackage);
        return new DependencyGraph(adjacencyList, cycles);
    }

    private void visit(String packageName) {
        VisitState state = states.get(packageName);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            addCycle(packageName);
            return;
        }

        states.put(packageName, VisitState.VISITING);
        currentPath.add(packageName);
        List<String> dependencies = new ArrayList<>();
        Set<String> uniqueNames = new LinkedHashSet<>();

        for (Dependency dependency : dependencyProvider.getDependencies(packageName)) {
            if (uniqueNames.add(dependency.name())) {
                dependencies.add(dependency.name());
            }
        }
        adjacencyList.put(packageName, List.copyOf(dependencies));

        for (String dependencyName : dependencies) {
            if (states.get(dependencyName) == VisitState.VISITING) {
                addCycle(dependencyName);
            } else {
                visit(dependencyName);
            }
        }

        currentPath.remove(currentPath.size() - 1);
        states.put(packageName, VisitState.VISITED);
    }

    private void addCycle(String repeatedPackage) {
        int start = currentPath.indexOf(repeatedPackage);
        if (start < 0) {
            return;
        }
        List<String> cycle = new ArrayList<>(currentPath.subList(start, currentPath.size()));
        cycle.add(repeatedPackage);
        String description = String.join(" -> ", cycle);
        if (!cycles.contains(description)) {
            cycles.add(description);
        }
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
