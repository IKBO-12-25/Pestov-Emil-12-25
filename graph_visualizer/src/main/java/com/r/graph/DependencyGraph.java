package com.r.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DependencyGraph(
        Map<String, List<String>> adjacencyList,
        List<String> cycles
) {
    public DependencyGraph {
        Map<String, List<String>> orderedGraph = new LinkedHashMap<>();
        adjacencyList.forEach((name, dependencies) ->
                orderedGraph.put(name, List.copyOf(dependencies))
        );
        adjacencyList = Collections.unmodifiableMap(orderedGraph);
        cycles = List.copyOf(cycles);
    }
}
