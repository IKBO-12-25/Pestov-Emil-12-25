package com.r.shell;

import com.r.logging.XmlCommandLogger;
import com.r.parser.CommandParser;
import com.r.parser.CommandParser.ParsedCommand;
import com.r.vfs.Vfs;
import com.r.vfs.VfsDirectory;
import com.r.vfs.VfsFile;
import com.r.vfs.VfsNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;

public class Shell {

    private static final int SINGLE_ITEM_LIMIT = 1;
    private static final int OPTION_PREFIX_LENGTH = 1;
    private static final int LAST_INDEX_OFFSET = 1;
    private static final int FIRST_ARGUMENT_INDEX = 0;

    private static final Map<Character, Set<UnameField>> UNAME_FLAGS = Map.of(
            'a', EnumSet.allOf(UnameField.class),
            's', EnumSet.of(UnameField.SYSTEM),
            'n', EnumSet.of(UnameField.HOSTNAME),
            'r', EnumSet.of(UnameField.RELEASE),
            'v', EnumSet.of(UnameField.VERSION),
            'm', EnumSet.of(UnameField.MACHINE),
            'p', EnumSet.of(UnameField.PROCESSOR),
            'i', EnumSet.of(UnameField.MACHINE),
            'o', EnumSet.of(UnameField.OS)
    );

    private final CommandParser parser;
    private final XmlCommandLogger logger;
    private final Vfs vfs;
    private final Map<String, Consumer<List<String>>> commands;

    private enum UnameField {
        SYSTEM,
        HOSTNAME,
        RELEASE,
        VERSION,
        MACHINE,
        PROCESSOR,
        OS
    }

    private record LsOptions(
            boolean showHidden,
            boolean longFormat,
            List<String> paths
    ) {}

    private record WcOptions(
            boolean countLines,
            boolean countWords,
            boolean countBytes,
            List<String> paths
    ) {}

    private record WcCounts(long lines, long words, long bytes) {
        private WcCounts add(WcCounts other) {
            return new WcCounts(
                    lines + other.lines,
                    words + other.words,
                    bytes + other.bytes
            );
        }
    }

    private record TreeOptions(boolean showHidden, String path) {}

    private record RmOptions(
            boolean recursive,
            boolean force,
            List<String> paths
    ) {}

    public Shell(XmlCommandLogger logger, Vfs vfs) {
        this.parser = new CommandParser();
        this.logger = logger;
        this.vfs = vfs;
        this.commands = createCommandMap();
    }

    private Map<String, Consumer<List<String>>> createCommandMap() {
        return Map.of(
                "ls", this::executeLs,
                "cd", this::executeCd,
                "uname", this::executeUname,
                "wc", this::executeWc,
                "tree", this::executeTree,
                "rm", this::executeRm
        );
    }

    private String getPrompt() {
        return vfs.getName() + ":" + vfs.getCurrentPath() + "$ ";
    }

    public void run(Path scriptPath) {
        if (scriptPath != null && !executeScript(scriptPath)) {
            return;
        }
        runInteractiveLoop();
    }

    private void runInteractiveLoop() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(getPrompt());
                if (!scanner.hasNextLine()) {
                    break;
                }
                if (!executeSafe(scanner.nextLine())) {
                    break;
                }
            }
        }
    }

    private boolean executeScript(Path scriptPath) {
        System.out.println("\nExecuting startup script...\n");
        if (!Files.isRegularFile(scriptPath)) {
            System.out.println("Ошибка: стартовый скрипт не найден: " + scriptPath);
            return true;
        }

        try {
            List<String> lines = Files.readAllLines(
                    scriptPath,
                    StandardCharsets.UTF_8
            );
            for (String line : lines) {
                if (!executeScriptLine(line)) {
                    return false;
                }
            }
        } catch (IOException exception) {
            System.out.println("Ошибка чтения скрипта: " + exception.getMessage());
        }

        System.out.println("\nStartup script finished.\n");
        return true;
    }

    private boolean executeScriptLine(String line) {
        if (line.isBlank() || line.stripLeading().startsWith("#")) {
            return true;
        }
        System.out.println(getPrompt() + line);
        return executeSafe(line);
    }

    private boolean executeSafe(String input) {
        try {
            return executeCommand(input);
        } catch (IllegalArgumentException exception) {
            System.out.println("Ошибка: " + exception.getMessage());
            return true;
        }
    }

    private boolean executeCommand(String input) {
        ParsedCommand parsed = parser.parse(input);
        if (parsed == null) {
            return true;
        }

        logCommand(parsed);
        String command = parsed.name();
        if (command.equals("exit")) {
            return executeExit(parsed.arguments());
        }

        Consumer<List<String>> handler = commands.get(command);
        if (handler == null) {
            System.out.println("Ошибка: неизвестная команда: " + command);
            return true;
        }

        handler.accept(parsed.arguments());
        return true;
    }

    private void logCommand(ParsedCommand parsed) {
        try {
            logger.log(parsed.name(), parsed.arguments());
        } catch (IOException exception) {
            System.out.println("Ошибка логирования: " + exception.getMessage());
        }
    }

    private boolean executeExit(List<String> arguments) {
        if (!arguments.isEmpty()) {
            System.out.println("Ошибка: команда exit не принимает аргументы.");
            return true;
        }
        System.out.println("Завершение работы.");
        return false;
    }

    private char[] extractFlags(String argument) {
        return argument.substring(OPTION_PREFIX_LENGTH).toCharArray();
    }

    private void executeLs(List<String> arguments) {
        LsOptions options = parseLsOptions(arguments);
        for (String path : options.paths()) {
            printLsTarget(path, options);
        }
    }

    private LsOptions parseLsOptions(List<String> arguments) {
        Set<Character> flags = new HashSet<>();
        List<String> paths = new ArrayList<>();

        for (String argument : arguments) {
            if (argument.equals("--")) {
                continue;
            }
            if (argument.startsWith("-")) {
                collectLsFlags(argument, flags);
            } else {
                paths.add(argument);
            }
        }
        if (paths.isEmpty()) {
            paths.add(".");
        }
        return new LsOptions(flags.contains('a'), flags.contains('l'), paths);
    }

    private void collectLsFlags(String argument, Set<Character> flags) {
        for (char flag : extractFlags(argument)) {
            if (flag != 'a' && flag != 'l') {
                throw new IllegalArgumentException("Неизвестный параметр ls: -" + flag);
            }
            flags.add(flag);
        }
    }

    private void printLsTarget(String path, LsOptions options) {
        VfsNode node = vfs.resolve(path);
        if (node == null) {
            System.out.println(
                    "ls: невозможно получить доступ к '" + path
                            + "': нет такого файла или каталога"
            );
            return;
        }
        if (node instanceof VfsFile file) {
            printLsEntry(file, options.longFormat());
            return;
        }
        if (node instanceof VfsDirectory directory) {
            printLsDirectory(path, directory, options);
        }
    }

    private void printLsDirectory(
            String path,
            VfsDirectory directory,
            LsOptions options
    ) {
        if (options.paths().size() > SINGLE_ITEM_LIMIT) {
            System.out.println(path + ":");
        }
        if (options.showHidden()) {
            printLsHiddenLinks(directory, options.longFormat());
        }
        for (VfsNode child : directory.getChildren()) {
            if (options.showHidden() || !child.getName().startsWith(".")) {
                printLsEntry(child, options.longFormat());
            }
        }
    }

    private void printLsHiddenLinks(VfsDirectory directory, boolean longFormat) {
        printLsEntry(directory, longFormat, ".");
        VfsDirectory parent = directory.getParent() != null
                ? directory.getParent()
                : directory;
        printLsEntry(parent, longFormat, "..");
    }

    private void printLsEntry(VfsNode node, boolean longFormat) {
        printLsEntry(node, longFormat, node.getName());
    }

    private void printLsEntry(
            VfsNode node,
            boolean longFormat,
            String displayName
    ) {
        if (!longFormat) {
            System.out.println(displayName);
            return;
        }
        String type = node.isDirectory() ? "d" : "-";
        long size = node instanceof VfsFile file ? file.getSize() : 0L;
        System.out.printf("%s %10d %s%n", type, size, displayName);
    }

    private void executeCd(List<String> arguments) {
        if (arguments.size() > SINGLE_ITEM_LIMIT) {
            throw new IllegalArgumentException("cd: слишком много аргументов");
        }
        String path = arguments.isEmpty()
                ? "/"
                : arguments.get(FIRST_ARGUMENT_INDEX);
        VfsNode node = vfs.resolve(path);

        if (node == null) {
            System.out.println("cd: каталог не найден: " + path);
            return;
        }
        if (!(node instanceof VfsDirectory)) {
            System.out.println("cd: не является каталогом: " + path);
            return;
        }
        vfs.changeDirectory(path);
    }

    private void executeUname(List<String> arguments) {
        EnumSet<UnameField> fields = parseUnameFields(arguments);
        Map<UnameField, String> values = collectSystemInfo();
        List<String> result = new ArrayList<>();

        for (UnameField field : fields) {
            result.add(values.get(field));
        }
        System.out.println(String.join(" ", result));
    }

    private EnumSet<UnameField> parseUnameFields(List<String> arguments) {
        if (arguments.isEmpty()) {
            return EnumSet.of(UnameField.SYSTEM);
        }
        EnumSet<UnameField> fields = EnumSet.noneOf(UnameField.class);
        for (String argument : arguments) {
            if (!argument.startsWith("-")) {
                throw new IllegalArgumentException(
                        "uname: неизвестный аргумент: " + argument
                );
            }
            collectUnameFlags(argument, fields);
        }
        return fields;
    }

    private void collectUnameFlags(
            String argument,
            EnumSet<UnameField> fields
    ) {
        for (char flag : extractFlags(argument)) {
            Set<UnameField> mapped = UNAME_FLAGS.get(flag);
            if (mapped == null) {
                throw new IllegalArgumentException(
                        "uname: неизвестный параметр: -" + flag
                );
            }
            fields.addAll(mapped);
        }
    }

    private Map<UnameField, String> collectSystemInfo() {
        String hostname = System.getenv().getOrDefault(
                "HOSTNAME",
                System.getenv().getOrDefault("COMPUTERNAME", "localhost")
        );
        String machine = System.getProperty("os.arch", "Unknown");
        return Map.of(
                UnameField.SYSTEM, System.getProperty("os.name", "Unknown"),
                UnameField.HOSTNAME, hostname,
                UnameField.RELEASE, System.getProperty("os.version", "Unknown"),
                UnameField.VERSION, System.getProperty("java.version", "Unknown"),
                UnameField.MACHINE, machine,
                UnameField.PROCESSOR, machine,
                UnameField.OS, "GNU/Linux"
        );
    }

    private void executeWc(List<String> arguments) {
        WcOptions options = parseWcOptions(arguments);
        WcCounts total = new WcCounts(0L, 0L, 0L);

        for (String path : options.paths()) {
            WcCounts counts = processWcFile(path, options);
            if (counts != null) {
                total = total.add(counts);
            }
        }
        if (options.paths().size() > SINGLE_ITEM_LIMIT) {
            printWcResult(total, "total", options);
        }
    }

    private WcOptions parseWcOptions(List<String> arguments) {
        Set<Character> flags = new HashSet<>();
        List<String> paths = new ArrayList<>();

        for (String argument : arguments) {
            if (argument.startsWith("-")) {
                collectWcFlags(argument, flags);
            } else {
                paths.add(argument);
            }
        }
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("wc: необходимо указать файл");
        }
        boolean showAll = flags.isEmpty();
        return new WcOptions(
                showAll || flags.contains('l'),
                showAll || flags.contains('w'),
                showAll || flags.contains('c'),
                paths
        );
    }

    private void collectWcFlags(String argument, Set<Character> flags) {
        for (char flag : extractFlags(argument)) {
            if (flag != 'l' && flag != 'w' && flag != 'c') {
                throw new IllegalArgumentException(
                        "wc: неизвестный параметр: -" + flag
                );
            }
            flags.add(flag);
        }
    }

    private WcCounts processWcFile(String path, WcOptions options) {
        VfsNode node = vfs.resolve(path);
        if (node == null) {
            System.out.println("wc: файл не найден: " + path);
            return null;
        }
        if (!(node instanceof VfsFile file)) {
            System.out.println("wc: не является файлом: " + path);
            return null;
        }
        WcCounts counts = countFileMetrics(file.getContent());
        printWcResult(counts, path, options);
        return counts;
    }

    private WcCounts countFileMetrics(byte[] content) {
        long lines = 0L;
        for (byte value : content) {
            if (value == '\n') {
                lines++;
            }
        }
        String text = new String(content, StandardCharsets.UTF_8).strip();
        long words = text.isEmpty() ? 0L : text.split("\\s+").length;
        return new WcCounts(lines, words, content.length);
    }

    private void printWcResult(
            WcCounts counts,
            String path,
            WcOptions options
    ) {
        StringBuilder result = new StringBuilder();
        if (options.countLines()) {
            result.append(String.format("%7d", counts.lines()));
        }
        if (options.countWords()) {
            result.append(String.format("%7d", counts.words()));
        }
        if (options.countBytes()) {
            result.append(String.format("%7d", counts.bytes()));
        }
        result.append(" ").append(path);
        System.out.println(result);
    }

    private void executeTree(List<String> arguments) {
        TreeOptions options = parseTreeOptions(arguments);
        VfsNode node = vfs.resolve(options.path());

        if (node == null) {
            System.out.println("tree: путь не найден: " + options.path());
            return;
        }
        System.out.println(node.getName());
        if (node instanceof VfsDirectory directory) {
            printTree(directory, "", options.showHidden());
        }
    }

    private TreeOptions parseTreeOptions(List<String> arguments) {
        boolean showHidden = false;
        String path = ".";
        boolean pathSpecified = false;

        for (String argument : arguments) {
            if (argument.startsWith("-")) {
                showHidden = parseTreeFlags(argument) || showHidden;
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
        return new TreeOptions(showHidden, path);
    }

    private boolean parseTreeFlags(String argument) {
        boolean showHidden = false;
        for (char flag : extractFlags(argument)) {
            if (flag != 'a') {
                throw new IllegalArgumentException(
                        "tree: неизвестный параметр: -" + flag
                );
            }
            showHidden = true;
        }
        return showHidden;
    }

    private void printTree(
            VfsDirectory directory,
            String prefix,
            boolean showHidden
    ) {
        List<VfsNode> children = filterVisibleChildren(directory, showHidden);
        int lastIndex = children.size() - LAST_INDEX_OFFSET;

        for (int i = 0; i < children.size(); i++) {
            VfsNode child = children.get(i);
            boolean last = i == lastIndex;
            String connector = last ? "└── " : "├── ";
            System.out.println(prefix + connector + child.getName());

            if (child instanceof VfsDirectory childDirectory) {
                String nextPrefix = prefix + (last ? "    " : "│   ");
                printTree(childDirectory, nextPrefix, showHidden);
            }
        }
    }

    private List<VfsNode> filterVisibleChildren(
            VfsDirectory directory,
            boolean showHidden
    ) {
        List<VfsNode> children = new ArrayList<>();
        for (VfsNode child : directory.getChildren()) {
            if (showHidden || !child.getName().startsWith(".")) {
                children.add(child);
            }
        }
        return children;
    }

    private void executeRm(List<String> arguments) {
        RmOptions options = parseRmOptions(arguments);
        for (String path : options.paths()) {
            removeEntry(path, options);
        }
    }

    private RmOptions parseRmOptions(List<String> arguments) {
        Set<Character> flags = new HashSet<>();
        List<String> paths = new ArrayList<>();
        boolean optionsEnabled = true;

        for (String argument : arguments) {
            if (optionsEnabled && argument.equals("--")) {
                optionsEnabled = false;
            } else if (isRmOption(argument, optionsEnabled)) {
                collectRmFlags(argument, flags);
            } else {
                paths.add(argument);
            }
        }
        if (paths.isEmpty()) {
            throw new IllegalArgumentException(
                    "rm: необходимо указать файл или каталог"
            );
        }
        return new RmOptions(flags.contains('r'), flags.contains('f'), paths);
    }

    private boolean isRmOption(String argument, boolean optionsEnabled) {
        return optionsEnabled && argument.startsWith("-") && !argument.equals("-");
    }

    private void collectRmFlags(String argument, Set<Character> flags) {
        for (char flag : extractFlags(argument)) {
            if (flag == 'r' || flag == 'R') {
                flags.add('r');
            } else if (flag == 'f') {
                flags.add('f');
            } else {
                throw new IllegalArgumentException(
                        "rm: неизвестный параметр: -" + flag
                );
            }
        }
    }

    private void removeEntry(String path, RmOptions options) {
        VfsNode node = vfs.resolve(path);
        if (node == null) {
            if (!options.force()) {
                System.out.println(
                        "rm: невозможно удалить '" + path
                                + "': нет такого файла или каталога"
                );
            }
            return;
        }
        if (!canRemoveNode(node, path, options.recursive())) {
            return;
        }
        if (!vfs.remove(path, options.recursive())) {
            System.out.println("rm: не удалось удалить: " + path);
        }
    }

    private boolean canRemoveNode(
            VfsNode node,
            String path,
            boolean recursive
    ) {
        if (node == vfs.getRoot()) {
            System.out.println("rm: запрещено удалять корневую директорию VFS");
            return false;
        }
        if (node instanceof VfsDirectory directory) {
            return canRemoveDirectory(directory, path, recursive);
        }
        return true;
    }

    private boolean canRemoveDirectory(
            VfsDirectory directory,
            String path,
            boolean recursive
    ) {
        if (vfs.contains(directory, vfs.getCurrentDirectory())) {
            System.out.println(
                    "rm: нельзя удалить текущий каталог или его родительскую директорию"
            );
            return false;
        }
        if (!recursive) {
            System.out.println(
                    "rm: невозможно удалить '" + path
                            + "': является каталогом (используйте -r)"
            );
            return false;
        }
        return true;
    }
}