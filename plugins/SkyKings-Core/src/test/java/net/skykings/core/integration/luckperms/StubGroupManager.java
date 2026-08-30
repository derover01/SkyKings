package net.skykings.core.integration.luckperms;

import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Handgeschriebenes {@link GroupManager}-Test-Double (siehe {@link StubLuckPerms} fuer den
 * Grund). Simuliert genau die Gruppen, die per {@link #addExistingGroup(String)} als
 * "in LuckPerms vorhanden" markiert wurden - alle anderen liefern {@code null} bei
 * {@link #getGroup(String)}, exakt wie das echte LuckPerms bei einer nicht geladenen Gruppe.
 */
final class StubGroupManager implements GroupManager {

    private final Set<String> existingGroups = new HashSet<>();

    void addExistingGroup(String name) {
        existingGroups.add(name);
    }

    @Override
    public Group getGroup(String name) {
        return existingGroups.contains(name) ? new StubGroup(name) : null;
    }

    @Override
    public CompletableFuture<Group> createAndLoadGroup(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<java.util.Optional<Group>> loadGroup(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> saveGroup(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> deleteGroup(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> loadAllGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Node> CompletableFuture<Map<String, java.util.Collection<T>>> searchAll(NodeMatcher<? extends T> matcher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<HeldNode<String>>> getWithPermission(String permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Group> getLoadedGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLoaded(String name) {
        throw new UnsupportedOperationException();
    }
}
