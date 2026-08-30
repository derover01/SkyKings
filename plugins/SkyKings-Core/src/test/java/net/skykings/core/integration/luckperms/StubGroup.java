package net.skykings.core.integration.luckperms;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.SortedSet;

final class StubGroup implements Group {

    private final String name;
    private final StubNodeMap data = new StubNodeMap();

    StubGroup(String name) {
        this.name = name;
    }

    @Override public String getName() { return name; }
    @Override public NodeMap data() { return data; }
    StubNodeMap stubData() { return data; }

    @Override public String getDisplayName() { throw new UnsupportedOperationException(); }
    @Override public String getDisplayName(QueryOptions queryOptions) { throw new UnsupportedOperationException(); }
    @Override public OptionalInt getWeight() { throw new UnsupportedOperationException(); }
    @Override public PermissionHolder.Identifier getIdentifier() { throw new UnsupportedOperationException(); }
    @Override public String getFriendlyName() { throw new UnsupportedOperationException(); }
    @Override public QueryOptions getQueryOptions() { throw new UnsupportedOperationException(); }
    @Override public CachedDataManager getCachedData() { throw new UnsupportedOperationException(); }
    @Override public NodeMap getData(DataType dataType) { throw new UnsupportedOperationException(); }
    @Override public NodeMap transientData() { throw new UnsupportedOperationException(); }
    @Override public SortedSet<Node> getDistinctNodes() { throw new UnsupportedOperationException(); }
    @Override public Collection<Node> resolveInheritedNodes(QueryOptions queryOptions) { throw new UnsupportedOperationException(); }
    @Override public SortedSet<Node> resolveDistinctInheritedNodes(QueryOptions queryOptions) { throw new UnsupportedOperationException(); }
    @Override public Collection<Group> getInheritedGroups(QueryOptions queryOptions) { throw new UnsupportedOperationException(); }
    @Override public void auditTemporaryNodes() { throw new UnsupportedOperationException(); }
}
