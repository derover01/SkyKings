package net.skykings.core.integration.luckperms;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Handgeschriebenes {@link NodeMap}-Test-Double (siehe {@link StubLuckPerms} fuer den Grund,
 * warum hier nicht Mockito verwendet wird). Haelt Nodes in einer echten, veraenderbaren Liste,
 * sodass Tests den tatsaechlichen End-Zustand pruefen koennen statt Aufrufe zu verifizieren.
 */
final class StubNodeMap implements NodeMap {

    private final List<Node> nodes = new ArrayList<>();

    void seed(Node... initial) {
        for (Node node : initial) {
            nodes.add(node);
        }
    }

    List<Node> currentNodes() {
        return nodes;
    }

    @Override
    public Collection<Node> toCollection() {
        return new ArrayList<>(nodes);
    }

    @Override
    public DataMutateResult add(Node node) {
        nodes.add(node);
        return DataMutateResult.SUCCESS;
    }

    @Override
    public DataMutateResult remove(Node node) {
        boolean removed = nodes.remove(node);
        return removed ? DataMutateResult.SUCCESS : DataMutateResult.FAIL_LACKS;
    }

    @Override
    public Map<ImmutableContextSet, Collection<Node>> toMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public net.luckperms.api.util.Tristate contains(Node node, NodeEqualityPredicate equalityPredicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataMutateResult.WithMergedNode add(Node node, TemporaryNodeMergeStrategy mergeStrategy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(Predicate<? super Node> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(ContextSet contextSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(ContextSet contextSet, Predicate<? super Node> predicate) {
        throw new UnsupportedOperationException();
    }
}
