package com.r.graph;

import com.r.dependency.Dependency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphBuilderTest {
    @Test
    void buildsTransitiveGraphAndDoesNotVisitSharedDependencyTwice() {
        Map<String, List<Dependency>> repository = Map.of(
                "A", dependencies("B", "C"),
                "B", dependencies("D"),
                "C", dependencies("D"),
                "D", List.of()
        );

        DependencyGraph graph = new DependencyGraphBuilder(
                name -> repository.getOrDefault(name, List.of())
        ).build("A");

        assertEquals(List.of("B", "C"), graph.adjacencyList().get("A"));
        assertEquals(List.of("D"), graph.adjacencyList().get("B"));
        assertEquals(4, graph.adjacencyList().size());
        assertTrue(graph.cycles().isEmpty());
    }

    @Test
    void detectsCycleAndStopsInfiniteRecursion() {
        Map<String, List<Dependency>> repository = Map.of(
                "A", dependencies("B"),
                "B", dependencies("C"),
                "C", dependencies("A")
        );

        DependencyGraph graph = new DependencyGraphBuilder(
                name -> repository.getOrDefault(name, List.of())
        ).build("A");

        assertEquals(3, graph.adjacencyList().size());
        assertEquals(List.of("A -> B -> C -> A"), graph.cycles());
    }

    @Test
    void handlesPackageWithoutDependencies() {
        DependencyGraph graph = new DependencyGraphBuilder(name -> List.of()).build("A");

        assertEquals(List.of(), graph.adjacencyList().get("A"));
        assertTrue(graph.cycles().isEmpty());
    }

    private List<Dependency> dependencies(String... names) {
        return java.util.Arrays.stream(names)
                .map(name -> new Dependency(name, name))
                .toList();
    }
}
