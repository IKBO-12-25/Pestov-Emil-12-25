
package com.r;

import com.r.config.AppConfig;
import com.r.logging.XmlCommandLogger;
import com.r.shell.Shell;

public class Main {

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.parse(args);

            printConfiguration(config);

            XmlCommandLogger logger = new XmlCommandLogger(config.logPath());

            Shell shell = new Shell(logger);

            shell.run(config.scriptPath());

        } catch (IllegalArgumentException exception) {
            System.err.println("Configuration error: " + exception.getMessage());

            System.exit(1);
        }
    }

    private static void printConfiguration(AppConfig config) {
        System.out.println("CLI emulator configuration:");
        System.out.println("VFS path: " + (config.vfsPath() == null ? "(not specified)" : config.vfsPath()));

        System.out.println("Log path: " + config.logPath());

        System.out.println("Startup script: " + (config.scriptPath() == null ? "(not specified)" : config.scriptPath()));

        System.out.println("Username: " + System.getProperty("user.name"));

        System.out.println();
    }
}
