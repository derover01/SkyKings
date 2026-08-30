package net.skykings.core.integration.luckperms;

import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Handgeschriebenes {@link UserManager}-Test-Double (siehe {@link StubLuckPerms} fuer den Grund). */
final class StubUserManager implements UserManager {

    private final UUID expectedUuid;
    private final User userToReturn;
    private int loadUserCallCount;
    private final List<User> savedUsers = new ArrayList<>();

    StubUserManager(UUID expectedUuid, User userToReturn) {
        this.expectedUuid = expectedUuid;
        this.userToReturn = userToReturn;
    }

    int getLoadUserCallCount() {
        return loadUserCallCount;
    }

    List<User> getSavedUsers() {
        return savedUsers;
    }

    @Override
    public CompletableFuture<User> loadUser(UUID uniqueId, String username) {
        loadUserCallCount++;
        if (!uniqueId.equals(expectedUuid)) {
            throw new IllegalArgumentException("Unerwartete UUID im Test: " + uniqueId);
        }
        return CompletableFuture.completedFuture(userToReturn);
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        savedUsers.add(user);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<UUID> lookupUniqueId(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<String> lookupUsername(UUID uniqueId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<PlayerSaveResult> savePlayerData(UUID uniqueId, String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> deletePlayerData(UUID uniqueId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Node> CompletableFuture<Map<UUID, java.util.Collection<T>>> searchAll(NodeMatcher<? extends T> matcher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<HeldNode<UUID>>> getWithPermission(String permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User getUser(UUID uniqueId) {
        return expectedUuid.equals(uniqueId) ? userToReturn : null;
    }

    @Override
    public User getUser(String username) {
        return userToReturn;
    }

    @Override
    public Set<User> getLoadedUsers() {
        return java.util.Collections.singleton(userToReturn);
    }

    @Override
    public boolean isLoaded(UUID uniqueId) {
        return expectedUuid.equals(uniqueId);
    }

    @Override
    public void cleanupUser(User user) {
    }
}
