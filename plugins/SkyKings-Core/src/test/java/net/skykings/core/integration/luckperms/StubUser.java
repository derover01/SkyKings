package net.skykings.core.integration.luckperms;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.util.Collection;
import java.util.SortedSet;
import java.util.UUID;

/** Handgeschriebenes {@link User}-Test-Double (siehe {@link StubLuckPerms} fuer den Grund). */
final class StubUser implements User {

    private final UUID uniqueId;
    private final StubNodeMap data;
    private String primaryGroup;

    StubUser(UUID uniqueId, StubNodeMap data, String initialPrimaryGroup) {
        this.uniqueId = uniqueId;
        this.data = data;
        this.primaryGroup = initialPrimaryGroup;
    }

    String getPrimaryGroupValue() {
        return primaryGroup;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getUsername() {
        return "Tester";
    }

    @Override
    public String getPrimaryGroup() {
        return primaryGroup;
    }

    @Override
    public DataMutateResult setPrimaryGroup(String group) {
        if (primaryGroup != null && primaryGroup.equalsIgnoreCase(group)) {
            return DataMutateResult.FAIL_ALREADY_HAS;
        }
        primaryGroup = group;
        return DataMutateResult.SUCCESS;
    }

    @Override
    public NodeMap data() {
        return data;
    }

    @Override
    public PermissionHolder.Identifier getIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFriendlyName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryOptions getQueryOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CachedDataManager getCachedData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeMap getData(DataType dataType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeMap transientData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<Node> getDistinctNodes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Node> resolveInheritedNodes(QueryOptions queryOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<Node> resolveDistinctInheritedNodes(QueryOptions queryOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Group> getInheritedGroups(QueryOptions queryOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void auditTemporaryNodes() {
        throw new UnsupportedOperationException();
    }
}
