package com.r.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\$\\{([A-Za-z_][A-Za-z0-9_]*)}|\\$([A-Za-z_][A-Za-z0-9_]*)"
    );
    private static final char NO_QUOTE = '\0';
    private static final int COMMAND_INDEX = 0;
    private static final int FIRST_ARGUMENT_INDEX = 1;

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
                tokens.get(COMMAND_INDEX),
                tokens.subList(FIRST_ARGUMENT_INDEX, tokens.size())
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
            String replacement = value == null ? "" : value;
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private List<String> tokenize(String input) {
        TokenizerState state = new TokenizerState();
        for (char currentChar : input.toCharArray()) {
            state.processChar(currentChar);
        }
        return state.finish();
    }

    private static final class TokenizerState {

        private final List<String> tokens = new ArrayList<>();
        private final StringBuilder current = new StringBuilder();
        private char quote = NO_QUOTE;
        private boolean escaped;
        private boolean tokenStarted;

        private void processChar(char currentChar) {
            if (escaped) {
                current.append(currentChar);
                escaped = false;
                tokenStarted = true;
                return;
            }
            if (isEscapeStart(currentChar)) {
                escaped = true;
                tokenStarted = true;
                return;
            }
            if (quote != NO_QUOTE) {
                handleQuotedChar(currentChar);
                return;
            }
            handleUnquotedChar(currentChar);
        }

        private boolean isEscapeStart(char currentChar) {
            return currentChar == '\\' && quote != '\'';
        }

        private void handleQuotedChar(char currentChar) {
            if (currentChar == quote) {
                quote = NO_QUOTE;
            } else {
                current.append(currentChar);
            }
            tokenStarted = true;
        }

        private void handleUnquotedChar(char currentChar) {
            if (currentChar == '\'' || currentChar == '"') {
                quote = currentChar;
                tokenStarted = true;
                return;
            }
            if (Character.isWhitespace(currentChar)) {
                flushToken();
                return;
            }
            current.append(currentChar);
            tokenStarted = true;
        }

        private void flushToken() {
            if (tokenStarted) {
                tokens.add(current.toString());
                current.setLength(0);
                tokenStarted = false;
            }
        }

        private List<String> finish() {
            if (escaped) {
                current.append('\\');
            }
            if (quote != NO_QUOTE) {
                throw new IllegalArgumentException("Unclosed quotation mark");
            }
            flushToken();
            return tokens;
        }
    }
}