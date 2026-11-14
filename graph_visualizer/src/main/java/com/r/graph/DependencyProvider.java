package com.r.graph;

import com.r.dependency.Dependency;

import java.util.List;

@FunctionalInterface
public interface DependencyProvider {
    List<Dependency> getDependencies(String packageName);
}
