package com.r.vfs;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class VfsDirectory extends VfsNode {

    private final Map<String, VfsNode> children = new LinkedHashMap<>();

    public VfsDirectory(String name, VfsDirectory parent) {
        super(name, parent);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    public void addChild(VfsNode node) {
        children.put(node.getName(), node);
    }

    public VfsNode removeChild(String name) {
        return children.remove(name);
    }

    public VfsNode getChild(String name) {
        return children.get(name);
    }

    public Collection<VfsNode> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    public boolean hasChild(String name) {
        return children.containsKey(name);
    }

    public int getChildrenCount() {
        return children.size();
    }
}
