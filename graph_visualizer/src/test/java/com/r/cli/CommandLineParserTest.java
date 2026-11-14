package com.r.cli;

import com.r.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandLineParserTest {
    private final CommandLineParser parser = new CommandLineParser();

    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesValidConfiguration() {
        AppConfig config = parser.parse(arguments(
                "requests", "https://pypi.org", "false", "graph.svg", "true"
        ));

        assertEquals("requests", config.packageName());
        assertEquals("https://pypi.org", config.repository());
        assertEquals(true, config.asciiTree());
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[0]));
    }

    @Test
    void rejectsUnknownOption() {
        String[] args = arguments(
                "requests", "https://pypi.org", "false", "graph.svg", "false"
        );
        args[0] = "--unknown";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    void rejectsInvalidPackageName() {
        String[] args = arguments(
                "bad package", "https://pypi.org", "false", "graph.svg", "false"
        );
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    void rejectsInvalidRepositoryUrl() {
        String[] args = arguments(
                "requests", "ftp://pypi.org", "false", "graph.svg", "false"
        );
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    void rejectsMissingTestRepositoryFile() {
        String[] args = arguments(
                "A", temporaryDirectory.resolve("missing.txt").toString(),
                "true", "graph.svg", "false"
        );
        assertThrows(IllegalArgumentException.class, () -> parser.parse(args));
    }

    @Test
    void acceptsExistingTestRepositoryFile() throws Exception {
        Path repository = Files.createFile(
                temporaryDirectory.resolve("repository.txt")
        );
        AppConfig config = parser.parse(arguments(
                "A", repository.toString(), "true", "graph.svg", "false"
        ));

        assertEquals(true, config.testMode());
    }

    @Test
    void rejectsInvalidBooleanAndOutputExtension() {
        String[] invalidBoolean = arguments(
                "requests", "https://pypi.org", "yes", "graph.svg", "false"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(invalidBoolean)
        );

        String[] invalidOutput = arguments(
                "requests", "https://pypi.org", "false", "graph.png", "false"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(invalidOutput)
        );
    }

    private String[] arguments(
            String packageName,
            String repository,
            String testMode,
            String output,
            String asciiTree
    ) {
        return new String[]{
                "--package", packageName,
                "--repository", repository,
                "--test-mode", testMode,
                "--output", output,
                "--ascii-tree", asciiTree
        };
    }
}
