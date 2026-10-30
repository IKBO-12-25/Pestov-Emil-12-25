package com.r.vfs;

public class VfsFile extends VfsNode {

    private final byte[] content;

    public VfsFile(
            String name,
            VfsDirectory parent,
            byte[] content
    ) {
        super(name, parent);
        this.content = content.clone();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public long getSize() {
        return content.length;
    }
}
