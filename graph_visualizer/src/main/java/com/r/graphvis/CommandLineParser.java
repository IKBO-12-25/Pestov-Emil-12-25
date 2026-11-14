package com.r.graphvis;

import com.r.config.AppConfig;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandLineParser {
    private static final Set<String> ALLOWED_OPTIONS = Set.of(
            "--package", "--repository", "--test-mode", "--output", "--ascii-tree"
    );

    /**
     * Преобразует аргументы командной строки в проверенную конфигурацию.
     */
    public AppConfig parse(String[] args) {
        Map<String, String> options = readOptions(args);
        String packageName = required(options, "--package");
        String repository = required(options, "--repository");
        boolean testMode = readBoolean(options, "--test-mode");
        String outputFile = required(options, "--output");
        boolean asciiTree = readBoolean(options, "--ascii-tree");

        validatePackage(packageName);
        validateRepository(repository, testMode);
        validateOutput(outputFile);

        return new AppConfig(
                packageName, repository, testMode, outputFile, asciiTree
        );
    }

    private Map<String, String> readOptions(String[] args) {
        if (args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Для каждой опции необходимо указать значение."
            );
        }

        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < args.length; index += 2) {
            String option = args[index];
            validateOption(option, options);
            options.put(option, args[index + 1]);
        }
        return options;
    }

    private void validateOption(String option, Map<String, String> options) {
        if (!ALLOWED_OPTIONS.contains(option)) {
            throw new IllegalArgumentException("Неизвестная опция: " + option);
        }
        if (options.containsKey(option)) {
            throw new IllegalArgumentException("Опция указана повторно: " + option);
        }
    }

    private String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задан параметр " + name + ".");
        }
        return value;
    }

    private boolean readBoolean(Map<String, String> options, String name) {
        String value = required(options, name);
        if (!value.equalsIgnoreCase("true")
                && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(
                    "Параметр " + name + " должен быть true или false."
            );
        }
        return Boolean.parseBoolean(value);
    }

    private void validatePackage(String packageName) {
        if (!packageName.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException(
                    "Некорректное имя пакета: " + packageName
            );
        }
    }

    private void validateRepository(String repository, boolean testMode) {
        if (testMode) {
            validateTestRepository(repository);
        } else {
            validateRepositoryUrl(repository);
        }
    }

    private void validateTestRepository(String repository) {
        Path path = Path.of(repository);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException(
                    "Файл тестового репозитория не существует или недоступен: "
                            + repository
            );
        }
    }

    private void validateRepositoryUrl(String repository) {
        try {
            URI uri = URI.create(repository);
            boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || uri.getHost() == null) {
                throw new IllegalArgumentException(
                        "Ожидается корректный HTTP(S) URL репозитория."
                );
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Некорректный URL репозитория: " + repository
            );
        }
    }

    private void validateOutput(String outputFile) {
        if (!outputFile.toLowerCase().endsWith(".svg")) {
            throw new IllegalArgumentException(
                    "Имя выходного файла должно иметь расширение .svg."
            );
        }
        Path path = Path.of(outputFile);
        Path parent = path.toAbsolutePath().getParent();
        if (path.getFileName() == null || parent == null
                || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException(
                    "Путь выходного файла указан некорректно."
            );
        }
    }

    public static String usage() {
        return "Использование:\n"
                + "  ./run.sh --package ИМЯ --repository URL_ИЛИ_ФАЙЛ\n"
                + "    --test-mode true|false --output ФАЙЛ.svg\n"
                + "    --ascii-tree true|false\n";
    }
}
