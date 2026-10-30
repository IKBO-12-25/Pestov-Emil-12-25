package com.r.shell;

import com.r.logging.XmlCommandLogger;
import com.r.parser.CommandParser;
import com.r.parser.CommandParser.ParsedCommand;
import com.r.vfs.Vfs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Shell {

    private final CommandParser parser;
    private final XmlCommandLogger logger;
    private final Vfs vfs;

    public Shell(XmlCommandLogger logger, Vfs vfs) {
        this.parser = new CommandParser();
        this.logger = logger;
        this.vfs = vfs;
    }

    private String getPrompt() {
        return vfs.getName() + ":" + vfs.getCurrentPath() + "$ ";
    }

    public void run(Path scriptPath) {
        if (scriptPath != null) {
            if (!executeScript(scriptPath)) {
                return;
            }
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(getPrompt());

                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine();

                try {
                    if (!executeCommand(input)) {
                        break;
                    }
                } catch (IllegalArgumentException exception) {
                    System.out.println(
                            "Ошибка: " + exception.getMessage()
                    );
                }
            }
        }
    }

    private boolean executeScript(Path scriptPath) {
        System.out.println();
        System.out.println("Executing startup script...");
        System.out.println();

        if (!Files.isRegularFile(scriptPath)) {
            System.out.println(
                    "Ошибка: стартовый скрипт не найден: " + scriptPath
            );
            return true;
        }

        try {
            for (String line : Files.readAllLines(
                    scriptPath,
                    StandardCharsets.UTF_8
            )) {

                if (line.isBlank() ||
                        line.stripLeading().startsWith("#")) {
                    continue;
                }

                System.out.println(getPrompt() + line);

                try {
                    if (!executeCommand(line)) {
                        return false;
                    }
                } catch (IllegalArgumentException exception) {
                    System.out.println(
                            "Ошибка: " + exception.getMessage()
                    );
                }
            }

        } catch (IOException exception) {
            System.out.println(
                    "Ошибка чтения скрипта: " + exception.getMessage()
            );
        }

        System.out.println();
        System.out.println("Startup script finished.");
        System.out.println();

        return true;
    }

    private boolean executeCommand(String input) {
        ParsedCommand parsed = parser.parse(input);

        if (parsed == null) {
            return true;
        }

        String command = parsed.name();

        try {
            logger.log(command, parsed.arguments());
        } catch (IOException exception) {
            System.out.println(
                    "Ошибка логирования: " + exception.getMessage()
            );
        }

        switch (command) {
            case "ls" -> System.out.println(
                    "Команда ls, аргументы: " + parsed.arguments()
            );

            case "cd" -> System.out.println(
                    "Команда cd, аргументы: " + parsed.arguments()
            );

            case "exit" -> {
                if (!parsed.arguments().isEmpty()) {
                    System.out.println(
                            "Ошибка: команда exit не принимает аргументы."
                    );
                    return true;
                }

                System.out.println("Завершение работы.");
                return false;
            }

            default -> System.out.println(
                    "Ошибка: неизвестная команда: " + command
            );
        }

        return true;
    }
}