package com.r;

import com.r.config.AppConfig;
import com.r.logging.XmlCommandLogger;
import com.r.shell.Shell;
import com.r.vfs.Vfs;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.parse(args);

            printConfiguration(config);

            Vfs vfs;

            if (config.vfsPath() == null) {
                vfs = Vfs.createDefault();
            } else {
                vfs = Vfs.load(config.vfsPath());
            }

            System.out.println("VFS loaded successfully.");
            System.out.println("VFS name: " + vfs.getName());
            System.out.println(
                    "Directories: " + vfs.getDirectoryCount()
            );
            System.out.println(
                    "Files: " + vfs.getFileCount()
            );
            System.out.println();

            XmlCommandLogger logger =
                    new XmlCommandLogger(config.logPath());

            Shell shell = new Shell(logger, vfs);

            shell.run(config.scriptPath());

        } catch (IllegalArgumentException | IOException exception) {
            System.err.println(
                    "Application error: " + exception.getMessage()
            );

            System.exit(1);
        }
    }

    private static void printConfiguration(AppConfig config) {
        System.out.println("CLI emulator configuration:");

        System.out.println(
                "VFS path: " +
                        (config.vfsPath() == null
                                ? "(in-memory default)"
                                : config.vfsPath())
        );

        System.out.println("Log path: " + config.logPath());

        System.out.println(
                "Startup script: " +
                        (config.scriptPath() == null
                                ? "(not specified)"
                                : config.scriptPath())
        );

        System.out.println(
                "Username: " + System.getProperty("user.name")
        );

        System.out.println();
    }
}
