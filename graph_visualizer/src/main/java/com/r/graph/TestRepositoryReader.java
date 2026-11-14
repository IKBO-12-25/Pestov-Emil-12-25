package com.r.graph;

import com.r.dependency.Dependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRepositoryReader {
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^([A-Z]+)\\s*->\\s*(.*)$"
    );

    /**
     * Загружает тестовый граф из текстового файла в формате A -> B, C.
     */
    public Map<String, List<Dependency>> read(Path file) {
        Map<String, List<Dependency>> repository = new LinkedHashMap<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Не удалось прочитать тестовый репозиторий: " + exception.getMessage(),
                    exception
            );
        }

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                throw invalidLine(index + 1);
            }

            String packageName = matcher.group(1);
            if (repository.containsKey(packageName)) {
                throw new IllegalArgumentException(
                        "Пакет " + packageName + " повторяется в строке " + (index + 1) + "."
                );
            }
            repository.put(packageName, parseDependencies(matcher.group(2), index + 1));
        }
        if (repository.isEmpty()) {
            throw new IllegalArgumentException("Файл тестового репозитория пуст.");
        }
        return repository;
    }

    private List<Dependency> parseDependencies(String value, int lineNumber) {
        List<Dependency> dependencies = new ArrayList<>();
        if (value.isBlank()) {
            return dependencies;
        }
        for (String item : value.split(",")) {
            String name = item.trim();
            if (!name.matches("[A-Z]+")) {
                throw invalidLine(lineNumber);
            }
            dependencies.add(new Dependency(name, name));
        }
        return List.copyOf(dependencies);
    }

    private IllegalArgumentException invalidLine(int lineNumber) {
        return new IllegalArgumentException(
                "Некорректная строка " + lineNumber
                        + ". Ожидается формат: A -> B, C. Используйте большие латинские буквы."
        );
    }
}
