package com.r.graph;

import java.util.List;

public record DependencyLoadOrder(List<String> packages, List<String> cycles) {
    public DependencyLoadOrder {
        packages = List.copyOf(packages);
        cycles = List.copyOf(cycles);
    }

    public boolean isPossible() {
        return cycles.isEmpty();
    }
}
