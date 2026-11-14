package com.r;

import com.r.graphvis.CommandLineParser;
import com.r.config.AppConfig;

public class Main {
    public static void main(String[] args) {
        try {
            AppConfig config = new CommandLineParser().parse(args);
            printConfig(config);
        } catch (IllegalArgumentException exception) {
            System.err.println("Ошибка: " + exception.getMessage());
            System.err.println(CommandLineParser.usage());
            System.exit(1);
        }
    }

    /**
     * Выводит параметры приложения в формате ключ-значение.
     */
    private static void printConfig(AppConfig config) {
        System.out.println("package = " + config.packageName());
        System.out.println("repository = " + config.repository());
        System.out.println("test-mode = " + config.testMode());
        System.out.println("output = " + config.outputFile());
        System.out.println("ascii-tree = " + config.asciiTree());
    }
}
