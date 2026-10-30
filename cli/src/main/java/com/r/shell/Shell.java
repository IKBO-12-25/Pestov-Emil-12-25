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
            case "ls" -> executeLs(parsed.arguments());

            case "cd" -> executeCd(parsed.arguments());

            case "uname" -> executeUname(parsed.arguments());

            case "wc" -> executeWc(parsed.arguments());

            case "tree" -> executeTree(parsed.arguments());

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


    private void executeLs(java.util.List<String> arguments) {
        boolean showHidden = false;
        boolean longFormat = false;

        java.util.List<String> paths = new java.util.ArrayList<>();

        for (String argument : arguments) {
            if (argument.startsWith("-")) {
                if (argument.equals("--")) {
                    continue;
                }

                for (int i = 1; i < argument.length(); i++) {
                    switch (argument.charAt(i)) {
                        case 'a' -> showHidden = true;
                        case 'l' -> longFormat = true;
                        default -> throw new IllegalArgumentException(
                                "Неизвестный параметр ls: -" +
                                        argument.charAt(i)
                        );
                    }
                }
            } else {
                paths.add(argument);
            }
        }

        if (paths.isEmpty()) {
            paths.add(".");
        }

        for (String path : paths) {
            com.r.vfs.VfsNode node = vfs.resolve(path);

            if (node == null) {
                System.out.println(
                        "ls: невозможно получить доступ к '" +
                                path + "': нет такого файла или каталога"
                );
                continue;
            }

            if (node instanceof com.r.vfs.VfsFile file) {
                printLsEntry(file, longFormat);
                continue;
            }

            if (node instanceof com.r.vfs.VfsDirectory directory) {
                if (paths.size() > 1) {
                    System.out.println(path + ":");
                }

                if (showHidden) {
                    printLsEntry(directory, longFormat, ".");

                    if (directory.getParent() != null) {
                        printLsEntry(
                                directory.getParent(),
                                longFormat,
                                ".."
                        );
                    } else {
                        printLsEntry(directory, longFormat, "..");
                    }
                }

                for (com.r.vfs.VfsNode child : directory.getChildren()) {
                    if (!showHidden && child.getName().startsWith(".")) {
                        continue;
                    }

                    printLsEntry(child, longFormat);
                }
            }
        }
    }

    private void printLsEntry(com.r.vfs.VfsNode node, boolean longFormat) {
        printLsEntry(node, longFormat, node.getName());
    }

    private void printLsEntry(
            com.r.vfs.VfsNode node,
            boolean longFormat,
            String displayName
    ) {
        if (!longFormat) {
            System.out.println(displayName);
            return;
        }

        String type = node.isDirectory() ? "d" : "-";

        long size = node instanceof com.r.vfs.VfsFile file
                ? file.getSize()
                : 0;

        System.out.printf(
                "%s %10d %s%n",
                type,
                size,
                displayName
        );
    }


    private void executeCd(java.util.List<String> arguments) {
        if (arguments.size() > 1) {
            throw new IllegalArgumentException(
                    "cd: слишком много аргументов"
            );
        }

        String path = arguments.isEmpty()
                ? "/"
                : arguments.get(0);

        com.r.vfs.VfsNode node = vfs.resolve(path);

        if (node == null) {
            System.out.println(
                    "cd: каталог не найден: " + path
            );
            return;
        }

        if (!(node instanceof com.r.vfs.VfsDirectory)) {
            System.out.println(
                    "cd: не является каталогом: " + path
            );
            return;
        }

        vfs.changeDirectory(path);
    }


    private void executeUname(java.util.List<String> arguments) {
        String systemName = System.getProperty("os.name", "Unknown");
        String hostname = System.getenv().getOrDefault(
                "HOSTNAME",
                System.getenv().getOrDefault("COMPUTERNAME", "localhost")
        );

        String release = System.getProperty("os.version", "Unknown");
        String version = System.getProperty("java.version", "Unknown");
        String machine = System.getProperty("os.arch", "Unknown");

        boolean showAll = arguments.contains("-a");
        boolean showSystem = showAll;
        boolean showHostname = false;
        boolean showRelease = false;
        boolean showVersion = false;
        boolean showMachine = false;
        boolean showProcessor = false;
        boolean showOperatingSystem = false;

        if (arguments.isEmpty()) {
            showSystem = true;
        }

        for (String argument : arguments) {
            if (!argument.startsWith("-")) {
                throw new IllegalArgumentException(
                        "uname: неизвестный аргумент: " + argument
                );
            }

            for (int i = 1; i < argument.length(); i++) {
                switch (argument.charAt(i)) {
                    case 'a' -> {
                        showSystem = true;
                        showHostname = true;
                        showRelease = true;
                        showVersion = true;
                        showMachine = true;
                        showProcessor = true;
                        showOperatingSystem = true;
                    }
                    case 's' -> showSystem = true;
                    case 'n' -> showHostname = true;
                    case 'r' -> showRelease = true;
                    case 'v' -> showVersion = true;
                    case 'm' -> showMachine = true;
                    case 'p' -> showProcessor = true;
                    case 'i' -> showMachine = true;
                    case 'o' -> showOperatingSystem = true;

                    default -> throw new IllegalArgumentException(
                            "uname: неизвестный параметр: -" +
                                    argument.charAt(i)
                    );
                }
            }
        }

        java.util.List<String> result = new java.util.ArrayList<>();

        if (showSystem) result.add(systemName);
        if (showHostname) result.add(hostname);
        if (showRelease) result.add(release);
        if (showVersion) result.add(version);
        if (showMachine) result.add(machine);
        if (showProcessor) result.add(machine);
        if (showOperatingSystem) result.add("GNU/Linux");

        System.out.println(String.join(" ", result));
    }


    private void executeWc(java.util.List<String> arguments) {
        boolean countLines = false;
        boolean countWords = false;
        boolean countBytes = false;

        java.util.List<String> paths = new java.util.ArrayList<>();

        for (String argument : arguments) {
            if (argument.startsWith("-")) {
                for (int i = 1; i < argument.length(); i++) {
                    switch (argument.charAt(i)) {
                        case 'l' -> countLines = true;
                        case 'w' -> countWords = true;
                        case 'c' -> countBytes = true;

                        default -> throw new IllegalArgumentException(
                                "wc: неизвестный параметр: -" +
                                        argument.charAt(i)
                        );
                    }
                }
            } else {
                paths.add(argument);
            }
        }

        if (paths.isEmpty()) {
            throw new IllegalArgumentException(
                    "wc: необходимо указать файл"
            );
        }

        if (!countLines && !countWords && !countBytes) {
            countLines = true;
            countWords = true;
            countBytes = true;
        }

        long totalLines = 0;
        long totalWords = 0;
        long totalBytes = 0;

        for (String path : paths) {
            com.r.vfs.VfsNode node = vfs.resolve(path);

            if (node == null) {
                System.out.println(
                        "wc: файл не найден: " + path
                );
                continue;
            }

            if (!(node instanceof com.r.vfs.VfsFile file)) {
                System.out.println(
                        "wc: не является файлом: " + path
                );
                continue;
            }

            byte[] content = file.getContent();

            long lines = 0;

            for (byte value : content) {
                if (value == '\n') {
                    lines++;
                }
            }

            String text = new String(
                    content,
                    java.nio.charset.StandardCharsets.UTF_8
            ).strip();

            long words = text.isEmpty()
                    ? 0
                    : text.split("\\s+").length;

            long bytes = content.length;

            totalLines += lines;
            totalWords += words;
            totalBytes += bytes;

            printWcResult(
                    lines,
                    words,
                    bytes,
                    path,
                    countLines,
                    countWords,
                    countBytes
            );
        }

        if (paths.size() > 1) {
            printWcResult(
                    totalLines,
                    totalWords,
                    totalBytes,
                    "total",
                    countLines,
                    countWords,
                    countBytes
            );
        }
    }

    private void printWcResult(
            long lines,
            long words,
            long bytes,
            String path,
            boolean countLines,
            boolean countWords,
            boolean countBytes
    ) {
        StringBuilder result = new StringBuilder();

        if (countLines) {
            result.append(String.format("%7d", lines));
        }

        if (countWords) {
            result.append(String.format("%7d", words));
        }

        if (countBytes) {
            result.append(String.format("%7d", bytes));
        }

        result.append(" ").append(path);

        System.out.println(result);
    }


    private void executeTree(java.util.List<String> arguments) {
        boolean showHidden = false;
        String path = ".";

        boolean pathSpecified = false;

        for (String argument : arguments) {
            if (argument.startsWith("-")) {
                for (int i = 1; i < argument.length(); i++) {
                    if (argument.charAt(i) == 'a') {
                        showHidden = true;
                    } else {
                        throw new IllegalArgumentException(
                                "tree: неизвестный параметр: -" +
                                        argument.charAt(i)
                        );
                    }
                }
            } else {
                if (pathSpecified) {
                    throw new IllegalArgumentException(
                            "tree: слишком много аргументов"
                    );
                }

                path = argument;
                pathSpecified = true;
            }
        }

        com.r.vfs.VfsNode node = vfs.resolve(path);

        if (node == null) {
            System.out.println(
                    "tree: путь не найден: " + path
            );
            return;
        }

        System.out.println(node.getName());

        if (node instanceof com.r.vfs.VfsDirectory directory) {
            printTree(directory, "", showHidden);
        }
    }

    private void printTree(
            com.r.vfs.VfsDirectory directory,
            String prefix,
            boolean showHidden
    ) {
        java.util.List<com.r.vfs.VfsNode> children =
                new java.util.ArrayList<>();

        for (com.r.vfs.VfsNode child : directory.getChildren()) {
            if (!showHidden && child.getName().startsWith(".")) {
                continue;
            }

            children.add(child);
        }

        for (int i = 0; i < children.size(); i++) {
            com.r.vfs.VfsNode child = children.get(i);

            boolean last = i == children.size() - 1;

            String connector = last ? "└── " : "├── ";

            System.out.println(prefix + connector + child.getName());

            if (child instanceof com.r.vfs.VfsDirectory childDirectory) {
                String nextPrefix = prefix + (last ? "    " : "│   ");

                printTree(
                        childDirectory,
                        nextPrefix,
                        showHidden
                );
            }
        }
    }
}