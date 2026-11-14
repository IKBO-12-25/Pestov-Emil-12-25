package com.r.config;

public record AppConfig(
        String packageName,
        String repository,
        boolean testMode,
        String outputFile,
        boolean asciiTree
) {
}
