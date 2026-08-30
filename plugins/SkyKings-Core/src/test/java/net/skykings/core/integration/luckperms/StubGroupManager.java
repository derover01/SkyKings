package net.skykings.core.integration.luckperms;

import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class StubGroupManager implements GroupManager {

    private final Map<String, StubGroup> groups = new LinkedHashMap<>();
    private final List<Group> savedGroups = new ArrayList<>();

    void addExistingGroup(String name) {
        groups.put(name, new StubGroup(name));
    }

    StubGroup getStubGroup(String name) {
        return groups.get(name);
    }

    List<Group> getSavedGroups() {
        return savedGroups;
    }

    @Override
    public Group getGroup(String name) {
        return groups.get(name);
    }

    @Override
    public CompletableFuture<Group> createAndLoadGroup(String name) {
        StubGroup group = new StubGroup(name);
        groups.put(name, group);
        return CompletableFuture.completedFuture(group);
    }

    @Override
    public CompletableFuture<java.util.Optional<Group>> loadGroup(String name) {
        return CompletableFuture.completedFuture(java.util.Optional.ofNullable(groups.get(name)));
    }

    @Override
    public CompletableFuture<Void> saveGroup(Group group) {
        savedGroups.add(group);
        return CompletableFuture.completedFuture(null);
    }

    @Override public CompletableFuture<Void> deleteGroup(Group group) { throw new UnsupportedOperationException(); }
    @Override public CompletableFuture<Void> loadAllGroups() { throw new UnsupportedOperationException(); }
    @Override public <T extends Node> CompletableFuture<Map<String, Collection<T>>> searchAll(NodeMatcher<? extends T> matcher) { throw new UnsupportedOperationException(); }
    @Override public CompletableFuture<List<HeldNode<String>>> getWithPermission(String permission) { throw new UnsupportedOperationException(); }
    @Override public Set<Group> getLoadedGroups() { return new java.util.HashSet<Group>(groups.values()); }
    @Override public boolean isLoaded(String name) { return groups.containsKey(name); }
}
