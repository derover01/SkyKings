package net.skykings.core.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.types.InheritanceNode;
import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.model.Rank;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LuckPermsPermissionBridgeTest {

    private StubGroupManager groupManager;
    private StubUserManager userManager;
    private StubUser user;
    private StubNodeMap data;
    private StubLuckPerms luckPerms;
    private UUID uuid;

    @Before
    public void setUp() throws Exception {
        uuid = UUID.randomUUID();
        groupManager = new StubGroupManager();
        data = new StubNodeMap();
        user = new StubUser(uuid, data, null);
        userManager = new StubUserManager(uuid, user);
        luckPerms = new StubLuckPerms(userManager, groupManager);
        registerProvider(luckPerms);
    }

    @After
    public void tearDown() throws Exception {
        unregisterProvider();
    }

    private static void registerProvider(LuckPerms instance) throws Exception {
        Method register = LuckPermsProvider.class.getDeclaredMethod("register", LuckPerms.class);
        register.setAccessible(true);
        register.invoke(null, instance);
    }

    private static void unregisterProvider() throws Exception {
        Method unregister = LuckPermsProvider.class.getDeclaredMethod("unregister");
        unregister.setAccessible(true);
        unregister.invoke(null);
    }

    private PermissionBridge bridge() {
        return new LuckPermsPermissionBridge(luckPerms, Logger.getLogger("test"));
    }

    private InheritanceNode inheritanceNode(String groupName) {
        return new StubInheritanceNode(groupName);
    }

    @Test
    public void syncRankProceedsWhenTargetGroupExists() {
        groupManager.addExistingGroup("knight");
        bridge().syncRank(uuid, Rank.KNIGHT);

        assertEquals(1, userManager.getLoadUserCallCount());
        assertEquals("knight", user.getPrimaryGroupValue());
        assertTrue(data.currentNodes().stream().anyMatch(n -> n instanceof InheritanceNode
                && ((InheritanceNode) n).getGroupName().equals("knight")));
        assertEquals(1, userManager.getSavedUsers().size());
    }

    @Test
    public void syncRankAutomaticallyCreatesMissingTargetGroup() {
        bridge().syncRank(uuid, Rank.KNIGHT);

        assertNotNull(groupManager.getGroup("knight"));
        assertEquals(1, userManager.getLoadUserCallCount());
        assertEquals("knight", user.getPrimaryGroupValue());
        assertTrue(data.currentNodes().stream().anyMatch(n -> n instanceof InheritanceNode
                && ((InheritanceNode) n).getGroupName().equals("knight")));
    }

    @Test
    public void syncRankRemovesOnlyOtherManagedRankGroups() {
        InheritanceNode otherSkyKingsGroup = inheritanceNode("gold");
        InheritanceNode foreignGroup = inheritanceNode("builder");
        data.seed(otherSkyKingsGroup, foreignGroup);
        groupManager.addExistingGroup("knight");

        bridge().syncRank(uuid, Rank.KNIGHT);

        assertFalse(data.currentNodes().contains(otherSkyKingsGroup));
        assertTrue(data.currentNodes().contains(foreignGroup));
    }

    @Test
    public void ownerGroupSurvivesGameplayRankSyncAndStaysPrimary() {
        InheritanceNode ownerNode = inheritanceNode("owner");
        data.seed(ownerNode);
        user = new StubUser(uuid, data, "owner");
        userManager = new StubUserManager(uuid, user);
        luckPerms = new StubLuckPerms(userManager, groupManager);
        groupManager.addExistingGroup("spieler");

        bridge().syncRank(uuid, Rank.SPIELER);

        assertTrue(data.currentNodes().contains(ownerNode));
        assertEquals("owner", user.getPrimaryGroupValue());
        assertTrue(data.currentNodes().stream().anyMatch(n -> n instanceof InheritanceNode
                && ((InheritanceNode) n).getGroupName().equals("spieler")));
    }

    @Test
    public void grantOwnerCreatesOwnerGroupAndWildcardAccess() {
        bridge().grantOwner(uuid);

        assertNotNull(groupManager.getGroup("owner"));
        assertEquals("owner", user.getPrimaryGroupValue());
        assertTrue(data.currentNodes().stream().anyMatch(n -> n instanceof InheritanceNode
                && ((InheritanceNode) n).getGroupName().equals("owner")));
        assertTrue(data.currentNodes().stream().anyMatch(n -> "*".equals(n.getKey()) && n.getValue()));
    }

    @Test
    public void syncRankDoesNotDuplicateOrResaveWhenAlreadyCorrect() {
        InheritanceNode alreadyKnight = inheritanceNode("knight");
        data.seed(alreadyKnight);
        groupManager.addExistingGroup("knight");
        user = new StubUser(uuid, data, "knight");
        userManager = new StubUserManager(uuid, user);
        luckPerms = new StubLuckPerms(userManager, groupManager);

        bridge().syncRank(uuid, Rank.KNIGHT);

        assertEquals(1, data.currentNodes().size());
        assertEquals(0, userManager.getSavedUsers().size());
    }

    @Test
    public void isAvailableIsAlwaysTrueForARealBridge() {
        assertTrue(bridge().isAvailable());
    }
}
