package com.r.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestRepositoryReaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsRepositoryDescription() throws Exception {
        Path file = temporaryDirectory.resolve("repository.txt");
        Files.writeString(file, "A -> B, C\nB -> D\nC -> D\nD ->\n");

        var repository = new TestRepositoryReader().read(file);

        assertEquals(2, repository.get("A").size());
        assertEquals("D", repository.get("B").get(0).name());
        assertEquals(0, repository.get("D").size());
    }

    @Test
    void rejectsLowercasePackageNames() throws Exception {
        Path file = temporaryDirectory.resolve("invalid.txt");
        Files.writeString(file, "A -> b\n");

        assertThrows(
                IllegalArgumentException.class,
                () -> new TestRepositoryReader().read(file)
        );
    }

    @Test
    void rejectsDuplicatePackageDescriptions() throws Exception {
        Path file = temporaryDirectory.resolve("duplicate.txt");
        Files.writeString(file, "A -> B\nA -> C\n");

        assertThrows(
                IllegalArgumentException.class,
                () -> new TestRepositoryReader().read(file)
        );
    }
}
