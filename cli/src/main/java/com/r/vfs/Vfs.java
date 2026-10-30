
package com.r.vfs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Vfs {

    private final VfsDirectory root;
    private VfsDirectory currentDirectory;

    private int directoryCount;
    private int fileCount;

    private Vfs(String name) {
        this.root = new VfsDirectory(name, null);
        this.currentDirectory = root;
        this.directoryCount = 1;
    }

    public static Vfs createDefault() {
        return new Vfs("memory");
    }

    public static Vfs load(Path source) throws IOException {
        if (Files.isSymbolicLink(source)) {
            throw new IOException(
                    "VFS source cannot be a symbolic link: " + source
            );
        }

        if (!Files.isDirectory(source)) {
            throw new IOException(
                    "VFS source is not a directory: " + source
            );
        }

        Path absoluteSource = source.toAbsolutePath().normalize();

        Path fileName = absoluteSource.getFileName();
        String name = fileName == null ? "root" : fileName.toString();

        Vfs vfs = new Vfs(name);

        Files.walkFileTree(
                absoluteSource,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attributes
                    ) throws IOException {
                        if (directory.equals(absoluteSource)) {
                            return FileVisitResult.CONTINUE;
                        }

                        Path relative = absoluteSource.relativize(directory);

                        VfsDirectory parent = findParent(vfs, relative);

                        VfsDirectory node = new VfsDirectory(
                                directory.getFileName().toString(),
                                parent
                        );

                        parent.addChild(node);
                        vfs.directoryCount++;

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        if (attributes.isSymbolicLink()) {
                            return FileVisitResult.CONTINUE;
                        }

                        Path relative = absoluteSource.relativize(file);

                        VfsDirectory parent = findParent(vfs, relative);

                        byte[] content = Files.readAllBytes(file);

                        VfsFile node = new VfsFile(
                                file.getFileName().toString(),
                                parent,
                                content
                        );

                        parent.addChild(node);
                        vfs.fileCount++;

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(
                            Path file,
                            IOException exception
                    ) throws IOException {
                        throw new IOException(
                                "Cannot read VFS entry: " + file,
                                exception
                        );
                    }
                }
        );

        return vfs;
    }

    private static VfsDirectory findParent(
            Vfs vfs,
            Path relative
    ) throws IOException {

        Path parentPath = relative.getParent();

        if (parentPath == null) {
            return vfs.root;
        }

        VfsDirectory current = vfs.root;

        for (Path part : parentPath) {
            VfsNode child = current.getChild(part.toString());

            if (!(child instanceof VfsDirectory directory)) {
                throw new IOException(
                        "Invalid directory structure: " + relative
                );
            }

            current = directory;
        }

        return current;
    }

    public VfsDirectory getRoot() {
        return root;
    }

    public VfsDirectory getCurrentDirectory() {
        return currentDirectory;
    }

    public String getName() {
        return root.getName();
    }

    public int getDirectoryCount() {
        return directoryCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public String getCurrentPath() {
        if (currentDirectory == root) {
            return "/";
        }

        StringBuilder path = new StringBuilder();

        VfsNode current = currentDirectory;

        while (current != null && current != root) {
            path.insert(0, "/" + current.getName());
            current = current.getParent();
        }

        return path.toString();
    }

    public VfsNode resolve(String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectory;
        }

        VfsNode current = path.startsWith("/")
                ? root
                : currentDirectory;

        String[] parts = path.split("/");

        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }

            if (part.equals("..")) {
                if (current.getParent() != null) {
                    current = current.getParent();
                }

                continue;
            }

            if (!(current instanceof VfsDirectory directory)) {
                return null;
            }

            current = directory.getChild(part);

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    public boolean changeDirectory(String path) {
        VfsNode node = resolve(path);

        if (!(node instanceof VfsDirectory directory)) {
            return false;
        }

        currentDirectory = directory;
        return true;
    }

    public boolean remove(String path, boolean recursive) {
        VfsNode node = resolve(path);

        if (node == null || node == root) {
            return false;
        }

        if (!(node instanceof VfsDirectory directory)) {
            VfsDirectory parent = node.getParent();

            if (parent == null) {
                return false;
            }

            parent.removeChild(node.getName());
            fileCount--;

            return true;
        }

        if (contains(directory, currentDirectory)) {
            return false;
        }

        if (!recursive) {
            return false;
        }

        int[] counts = countNodes(directory);

        VfsDirectory parent = directory.getParent();

        if (parent == null) {
            return false;
        }

        parent.removeChild(directory.getName());

        directoryCount -= counts[0];
        fileCount -= counts[1];

        return true;
    }

    private int[] countNodes(VfsDirectory directory) {
        int directories = 1;
        int files = 0;

        for (VfsNode child : directory.getChildren()) {
            if (child instanceof VfsDirectory childDirectory) {
                int[] childCounts = countNodes(childDirectory);

                directories += childCounts[0];
                files += childCounts[1];
            } else {
                files++;
            }
        }

        return new int[]{directories, files};
    }

    public boolean contains(VfsDirectory directory, VfsDirectory target) {
        VfsDirectory current = target;

        while (current != null) {
            if (current == directory) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

}
