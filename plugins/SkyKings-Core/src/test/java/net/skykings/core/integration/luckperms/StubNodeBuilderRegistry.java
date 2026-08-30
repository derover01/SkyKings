package net.skykings.core.integration.luckperms;

import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.RegexPermissionNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;

/** Handgeschriebenes {@link NodeBuilderRegistry}-Test-Double - nur {@code forInheritance()} wird gebraucht. */
final class StubNodeBuilderRegistry implements NodeBuilderRegistry {

    @Override
    public InheritanceNode.Builder forInheritance() {
        return new StubInheritanceNodeBuilder();
    }

    @Override
    public NodeBuilder<?, ?> forKey(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PermissionNode.Builder forPermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RegexPermissionNode.Builder forRegexPermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrefixNode.Builder forPrefix() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SuffixNode.Builder forSuffix() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetaNode.Builder forMeta() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WeightNode.Builder forWeight() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DisplayNameNode.Builder forDisplayName() {
        throw new UnsupportedOperationException();
    }
}
