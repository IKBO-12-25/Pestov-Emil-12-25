package com.r;

import com.r.cli.CommandLineParser;
import com.r.config.AppConfig;
import com.r.dependency.Dependency;
import com.r.dependency.PypiClient;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            AppConfig config = new CommandLineParser().parse(args);
            printConfig(config);
            if (!config.testMode()) {
                List<Dependency> dependencies = new PypiClient()
                        .fetchDependencies(config.repository(), config.packageName());
                printDependencies(dependencies);
            } else {
                System.out.println(
                        "Тестовый режим будет реализован на этапе 3."
                );
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println("Ошибка: " + exception.getMessage());
            System.err.println(CommandLineParser.usage());
            System.exit(1);
        }
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
     * Выводит найденные прямые зависимости пакета.
     */
    private static void printDependencies(List<Dependency> dependencies) {
        System.out.println("\nПрямые зависимости:");
        if (dependencies.isEmpty()) {
            System.out.println("У пакета нет прямых зависимостей.");
            return;
        }
        for (Dependency dependency : dependencies) {
            System.out.println("- " + dependency.requirement());
        }
    }
}
