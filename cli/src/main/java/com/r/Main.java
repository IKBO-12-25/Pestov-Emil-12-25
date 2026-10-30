
package com.r;

import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final String VFS_NAME = "cli";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(VFS_NAME + ":/$ ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                input = expandVariables(input);

                String[] parts = input.split("\\s+");
                String command = parts[0];
                String[] commandArgs = Arrays.copyOfRange(
                        parts, 1, parts.length
                );

                switch (command) {
                    case "ls" -> {
                        System.out.println(
                                "Команда ls, аргументы: "
                                        + Arrays.toString(commandArgs)
                        );
                    }
                    case "cd" -> {
                        System.out.println(
                                "Команда cd, аргументы: "
                                        + Arrays.toString(commandArgs)
                        );
                    }
                    case "exit" -> {
                        System.out.println("Завершение работы.");
                        return;
                    }
                    default -> System.out.println(
                            "Ошибка: неизвестная команда: " + command
                    );
                }
            }
        }
    }

    private static String expandVariables(String input) {
        Map<String, String> environment = System.getenv();

        for (Map.Entry<String, String> entry : environment.entrySet()) {
            input = input.replace(
                    "$" + entry.getKey(),
                    entry.getValue()
            );
        }

        return input;
    }
}
