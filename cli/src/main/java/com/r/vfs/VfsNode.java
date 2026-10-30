package com.r.vfs;

public abstract class VfsNode {

    private final String name;
    private final VfsDirectory parent;

    protected VfsNode(String name, VfsDirectory parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public VfsDirectory getParent() {
        return parent;
    }

    public abstract boolean isDirectory();
}
