
package com.r.config;

import java.nio.file.Path;

public record AppConfig(
        Path vfsPath,
        Path logPath,
        Path scriptPath
) {

    public static AppConfig parse(String[] args) {
        Path vfsPath = null;
        Path logPath = Path.of("logs", "cli.xml");
        Path scriptPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--vfs" -> {
                    vfsPath = Path.of(requireValue(args, ++i, "--vfs"));
                }
                case "--log" -> {
                    logPath = Path.of(requireValue(args, ++i, "--log"));
                }
                case "--script" -> {
                    scriptPath = Path.of(requireValue(args, ++i, "--script"));
                }
                default -> throw new IllegalArgumentException(
                        "Unknown parameter: " + args[i]
                );
            }
        }

        return new AppConfig(vfsPath, logPath, scriptPath);
    }

    private static String requireValue(
            String[] args,
            int index,
            String parameter
    ) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(
                    "Missing value for parameter: " + parameter
            );
        }

        return args[index];
    }
}
