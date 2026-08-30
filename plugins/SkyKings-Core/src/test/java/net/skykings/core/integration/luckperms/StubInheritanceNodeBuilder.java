package net.skykings.core.integration.luckperms;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.InheritanceNode;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;

/**
 * Handgeschriebenes {@link InheritanceNode.Builder}-Test-Double. Alle Konfigurationsmethoden
 * sind No-Ops (unsere Produktionslogik ruft nur {@code group(String)} und {@code build()} auf) -
 * siehe {@link StubLuckPerms} fuer den Gesamtkontext.
 */
final class StubInheritanceNodeBuilder implements InheritanceNode.Builder {

    private String groupName;

    @Override
    public InheritanceNode.Builder group(String group) {
        this.groupName = group;
        return this;
    }

    @Override
    public InheritanceNode.Builder group(Group group) {
        this.groupName = group.getName();
        return this;
    }

    @Override
    public InheritanceNode build() {
        return new StubInheritanceNode(groupName);
    }

    @Override
    public InheritanceNode.Builder value(boolean value) {
        return this;
    }

    @Override
    public InheritanceNode.Builder negated(boolean negated) {
        return this;
    }

    @Override
    public InheritanceNode.Builder expiry(long duration) {
        return this;
    }

    @Override
    public InheritanceNode.Builder expiry(TemporalAccessor temporalAccessor) {
        return this;
    }

    @Override
    public InheritanceNode.Builder expiry(TemporalAmount temporalAmount) {
        return this;
    }

    @Override
    public InheritanceNode.Builder clearExpiry() {
        return this;
    }

    @Override
    public InheritanceNode.Builder context(ContextSet contextSet) {
        return this;
    }

    @Override
    public InheritanceNode.Builder withContext(String key, String value) {
        return this;
    }

    @Override
    public InheritanceNode.Builder withContext(ContextSet contextSet) {
        return this;
    }

    @Override
    public <T> InheritanceNode.Builder withMetadata(NodeMetadataKey<T> key, T value) {
        return this;
    }
}
