package com.r;

import com.r.cli.CommandLineParser;
import com.r.config.AppConfig;
import com.r.dependency.Dependency;
import com.r.dependency.PypiClient;
import com.r.graph.DependencyGraph;
import com.r.graph.DependencyGraphBuilder;
import com.r.graph.DependencyProvider;
import com.r.graph.TestRepositoryReader;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            AppConfig config = new CommandLineParser().parse(args);
            printConfig(config);
            DependencyProvider provider = createProvider(config);
            DependencyGraph graph = new DependencyGraphBuilder(provider)
                    .build(config.packageName());
            printGraph(graph);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println("Ошибка: " + exception.getMessage());
            System.err.println(CommandLineParser.usage());
            System.exit(1);
        }
    }

    private static DependencyProvider createProvider(AppConfig config) {
        if (config.testMode()) {
            Map<String, List<Dependency>> repository = new TestRepositoryReader()
                    .read(Path.of(config.repository()));
            if (!repository.containsKey(config.packageName())) {
                throw new IllegalArgumentException(
                        "Пакет не найден в тестовом репозитории: " + config.packageName()
                );
            }
            return packageName -> repository.getOrDefault(packageName, List.of());
        }
        PypiClient client = new PypiClient();
        return packageName -> client.fetchDependencies(config.repository(), packageName);
    }

    /**
     * Выводит параметры приложения в формате ключ-значение.
     */
    private static void printConfig(AppConfig config) {
        System.out.println("package = " + config.packageName());
        System.out.println("repository = " + config.repository());
        System.out.println("test-mode = " + config.testMode());
        System.out.println("output = " + config.outputFile());
        System.out.println("ascii-tree = " + config.asciiTree());
    }

    /**
     * Выводит построенный граф и обнаруженные циклические зависимости.
     */
    private static void printGraph(DependencyGraph graph) {
        System.out.println("\nГраф зависимостей:");
        for (Map.Entry<String, List<String>> entry : graph.adjacencyList().entrySet()) {
            String dependencies = entry.getValue().isEmpty()
                    ? "нет"
                    : String.join(", ", entry.getValue());
            System.out.println(entry.getKey() + " -> " + dependencies);
        }
        if (graph.cycles().isEmpty()) {
            System.out.println("Циклические зависимости не обнаружены.");
        } else {
            System.out.println("Обнаружены циклические зависимости:");
            for (String cycle : graph.cycles()) {
                System.out.println("- " + cycle);
            }
        }
    }
}
