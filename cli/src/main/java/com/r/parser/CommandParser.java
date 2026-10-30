
package com.r.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}|\\$([A-Za-z_][A-Za-z0-9_]*)");

    public record ParsedCommand(
            String name,
            List<String> arguments
    ) {}

    public ParsedCommand parse(String input) {
        String expanded = expandVariables(input);
        List<String> tokens = tokenize(expanded);

        if (tokens.isEmpty()) {
            return null;
        }

        return new ParsedCommand(
                tokens.get(0),
                tokens.subList(1, tokens.size())
        );
    }

    private String expandVariables(String input) {
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1) != null
                    ? matcher.group(1)
                    : matcher.group(2);

            String value = System.getenv(variableName);

            if (value == null) {
                value = "";
            }

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(value)
            );
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        char quote = 0;
        boolean escaped = false;
        boolean tokenStarted = false;

        for (char c : input.toCharArray()) {

            if (escaped) {
                current.append(c);
                escaped = false;
                tokenStarted = true;
                continue;
            }

            if (c == '\\' && quote != '\'') {
                escaped = true;
                tokenStarted = true;
                continue;
            }

            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }

                tokenStarted = true;
                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c;
                tokenStarted = true;
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }

                continue;
            }

            current.append(c);
            tokenStarted = true;
        }

        if (escaped) {
            current.append('\\');
        }

        if (quote != 0) {
            throw new IllegalArgumentException(
                    "Unclosed quotation mark"
            );
        }

        if (tokenStarted) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}
