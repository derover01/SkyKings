package net.skykings.core.integration.luckperms;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.InheritanceNode;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Handgeschriebenes {@link InheritanceNode}-Test-Double (siehe {@link StubLuckPerms} fuer den
 * Grund: {@code ScopedNode#getType()} hat eine generische, kovariante Rueckgabe, die denselben
 * JDK-8-Reflection-Bug wie bei {@code LuckPerms}/{@code GroupManager} ausloest). Nur
 * {@link #getGroupName()} wird von {@link LuckPermsPermissionBridge} tatsaechlich genutzt.
 */
final class StubInheritanceNode implements InheritanceNode {

    private final String groupName;

    StubInheritanceNode(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public NodeType<InheritanceNode> getType() {
        return NodeType.INHERITANCE;
    }

    @Override
    public Builder toBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> resolveShorthand() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasExpiry() {
        return false;
    }

    @Override
    public Instant getExpiry() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasExpired() {
        return false;
    }

    @Override
    public Duration getExpiryDuration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableContextSet getContexts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> getMetadata(NodeMetadataKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Node other, NodeEqualityPredicate equalityPredicate) {
        throw new UnsupportedOperationException();
    }
}
