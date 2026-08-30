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
import static org.junit.Assert.assertTrue;

/**
 * Testet {@link LuckPermsPermissionBridge} gegen handgeschriebene Test-Doubles fuer die
 * LuckPerms-API (siehe {@link StubLuckPerms} fuer den Grund: Mockito kann mehrere LuckPerms-
 * API-Interfaces auf diesem JDK-8-Build wegen eines bekannten Reflection-Bugs
 * (TypeAnnotationParser NPE bei generischen, typannotierten Methoden) nicht mocken).
 *
 * <p>{@code InheritanceNode.builder(...)} ruft intern statisch {@code LuckPermsProvider.get()}
 * auf (echtes LuckPerms-Plugin-Verhalten) - dafuer wird hier per Reflection (die eigentliche
 * {@code register}/{@code unregister}-Methode ist package-private) das Test-Double als
 * "aktive LuckPerms-Instanz" registriert, exakt wie es das echte Plugin beim Start taete.
 */
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
    public void syncRankDoesNothingWhenTargetGroupIsMissing() {
        // "knight" bewusst NICHT als existierende Gruppe registriert.
        data.seed(inheritanceNode("gold"));

        bridge().syncRank(uuid, Rank.KNIGHT);

        assertEquals("Der Nutzer darf bei fehlender Zielgruppe gar nicht erst geladen werden",
                0, userManager.getLoadUserCallCount());
        assertEquals(0, userManager.getSavedUsers().size());
    }

    @Test
    public void syncRankWithMissingTargetGroupNeverRemovesExistingSkyKingsGroups() {
        InheritanceNode goldNode = inheritanceNode("gold");
        data.seed(goldNode);
        // "knight" bewusst NICHT als existierende Gruppe registriert.

        bridge().syncRank(uuid, Rank.KNIGHT);

        assertTrue("Bestehende Gruppen duerfen bei fehlender Zielgruppe nicht entfernt werden",
                data.currentNodes().contains(goldNode));
        assertEquals(null, user.getPrimaryGroupValue());
    }

    @Test
    public void syncRankNeverTouchesForeignOrTeamGroups() {
        InheritanceNode otherSkyKingsGroup = inheritanceNode("gold");
        InheritanceNode foreignGroup = inheritanceNode("builder");
        data.seed(otherSkyKingsGroup, foreignGroup);
        groupManager.addExistingGroup("knight");

        bridge().syncRank(uuid, Rank.KNIGHT);

        assertFalse("Die andere SkyKings-Ranggruppe muss entfernt worden sein",
                data.currentNodes().contains(otherSkyKingsGroup));
        assertTrue("Eine fremde/Team-Gruppe darf niemals entfernt werden",
                data.currentNodes().contains(foreignGroup));
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

        assertEquals("Keine zweite Inheritance-Node fuer dieselbe Gruppe", 1, data.currentNodes().size());
        assertEquals(0, userManager.getSavedUsers().size());
    }

    @Test
    public void isAvailableIsAlwaysTrueForARealBridge() {
        assertTrue(bridge().isAvailable());
    }
}
