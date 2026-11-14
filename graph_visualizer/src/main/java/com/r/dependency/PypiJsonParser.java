package com.r.dependency;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PypiJsonParser {
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "\"requires_dist\"\\s*:\\s*(null|\\[)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^\\s*([A-Za-z0-9][A-Za-z0-9._-]*)"
    );

    /**
     * Извлекает прямые зависимости из поля requires_dist ответа PyPI.
     */
    public List<Dependency> parseDependencies(String json) {
        Matcher field = FIELD_PATTERN.matcher(json);
        if (!field.find()) {
            throw new IllegalArgumentException(
                    "В ответе PyPI отсутствует поле requires_dist."
            );
        }
        if ("null".equalsIgnoreCase(field.group(1))) {
            return List.of();
        }

        List<String> requirements = readStringArray(json, field.end());
        List<Dependency> dependencies = new ArrayList<>();
        for (String requirement : requirements) {
            dependencies.add(toDependency(requirement));
        }
        return List.copyOf(dependencies);
    }

    private List<String> readStringArray(String json, int start) {
        List<String> values = new ArrayList<>();
        int index = skipSpaces(json, start);
        while (index < json.length() && json.charAt(index) != ']') {
            if (json.charAt(index) == ',') {
                index = skipSpaces(json, index + 1);
                continue;
            }
            if (json.charAt(index) != '"') {
                throw new IllegalArgumentException(
                        "Некорректный формат списка requires_dist."
                );
            }
            StringValue value = readString(json, index + 1);
            values.add(value.value());
            index = skipSpaces(json, value.nextIndex());
        }
        if (index >= json.length()) {
            throw new IllegalArgumentException(
                    "Список requires_dist не закрыт."
            );
        }
        return values;
    }

    private StringValue readString(String json, int index) {
        StringBuilder value = new StringBuilder();
        while (index < json.length()) {
            char current = json.charAt(index++);
            if (current == '"') {
                return new StringValue(value.toString(), index);
            }
            if (current == '\\') {
                index = appendEscaped(json, index, value);
            } else {
                value.append(current);
            }
        }
        throw new IllegalArgumentException("Незакрытая строка в JSON.");
    }

    private int appendEscaped(String json, int index, StringBuilder value) {
        if (index >= json.length()) {
            throw new IllegalArgumentException("Некорректная escape-последовательность JSON.");
        }
        char escaped = json.charAt(index++);
        switch (escaped) {
            case '"', '\\', '/' -> value.append(escaped);
            case 'b' -> value.append('\b');
            case 'f' -> value.append('\f');
            case 'n' -> value.append('\n');
            case 'r' -> value.append('\r');
            case 't' -> value.append('\t');
            case 'u' -> {
                if (index + 4 > json.length()) {
                    throw new IllegalArgumentException("Некорректный Unicode escape в JSON.");
                }
                value.append((char) Integer.parseInt(json.substring(index, index + 4), 16));
                index += 4;
            }
            default -> throw new IllegalArgumentException("Неизвестная escape-последовательность JSON.");
        }
        return index;
    }

    private Dependency toDependency(String requirement) {
        Matcher matcher = NAME_PATTERN.matcher(requirement);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Не удалось определить имя зависимости: " + requirement
            );
        }
        return new Dependency(matcher.group(1), requirement);
    }

    private int skipSpaces(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private record StringValue(String value, int nextIndex) {
    }
}
