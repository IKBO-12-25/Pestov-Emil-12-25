package com.r.dependency;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PypiJsonParserTest {
    private final PypiJsonParser parser = new PypiJsonParser();

    @Test
    void extractsDirectDependencies() {
        String json = """
                {
                  "info": {
                    "requires_dist": [
                      "charset-normalizer<4,>=2",
                      "idna<4,>=2.5",
                      "urllib3<3,>=1.21.1; python_version >= '3.9'"
                    ]
                  }
                }
                """;

        List<Dependency> result = parser.parseDependencies(json);

        assertEquals(3, result.size());
        assertEquals("charset-normalizer", result.get(0).name());
        assertEquals(
                "urllib3<3,>=1.21.1; python_version >= '3.9'",
                result.get(2).requirement()
        );
    }

    @Test
    void returnsEmptyListWhenRequiresDistIsNull() {
        assertEquals(
                List.of(),
                parser.parseDependencies("{\"info\":{\"requires_dist\":null}}")
        );
    }

    @Test
    void rejectsMissingRequiresDistField() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseDependencies("{\"info\":{}}")
        );
    }

    @Test
    void readsEscapedCharacters() {
        List<Dependency> result = parser.parseDependencies(
                "{\"requires_dist\":[\"sample>=1; extra == \\\"full\\\"\"]}"
        );

        assertEquals(1, result.size());
        assertEquals("sample", result.get(0).name());
    }
}
