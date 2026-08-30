package net.skykings.core.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.skykings.core.integration.NoOpPermissionBridge;
import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** LuckPerms-Anbindung. Gameplay-Rang und Teamrang bleiben strikt getrennt. */
public final class LuckPermsPermissionBridge implements PermissionBridge {

    private static final String OWNER_GROUP = "owner";
    private static final Set<String> TEAM_GROUPS = new HashSet<String>(Arrays.asList(
            "builder", "azubi", "testsupporter", "supporter", "srsupporter",
            "moderator", "srmoderator", "headofmods", "admin", "headadmin",
            "superadmin", "manager", "stvowner", "owner"));
    private static final Set<String> ADMIN_PLUS_GROUPS = new HashSet<String>(Arrays.asList(
            "admin", "headadmin", "superadmin", "manager", "stvowner"));
    private static final List<String> TEAM_PERMISSIONS = Arrays.asList(
            "skykings.staff.announcement",
            "skykings.staff.clearchat"
    );
    private static final List<String> ADMIN_PERMISSIONS = Arrays.asList(
            "skykings.admin.commands",
            "skykings.admin.freesign",
            "skykings.admin.crate",
            "skykings.admin.gutscheine",
            "skykings.admin.rang",
            "skykings.admin.rechte",
            "skykings.staff.gamemode"
    );

    public static PermissionBridge createIfAvailable(Logger logger) {
        RegisteredServiceProvider<LuckPerms> registration = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) return new NoOpPermissionBridge();
        LuckPermsPermissionBridge bridge = new LuckPermsPermissionBridge(registration.getProvider(), logger);
        bridge.ensureServerGroups();
        return bridge;
    }

    private final LuckPerms luckPerms;
    private final Logger logger;

    LuckPermsPermissionBridge(LuckPerms luckPerms, Logger logger) {
        this.luckPerms = Objects.requireNonNull(luckPerms, "luckPerms");
        this.logger = logger;
    }

    @Override public boolean isAvailable() { return true; }

    @Override
    public void syncRank(UUID uuid, Rank rank) {
        String targetGroup = RankGroupMapping.groupNameFor(rank);
        if (targetGroup == null) {
            logger.warning("Kein LuckPerms-Gruppen-Mapping für Rank " + rank + " definiert.");
            return;
        }
        ensureGroup(targetGroup, false).thenCompose(ignored -> {
            UserManager userManager = luckPerms.getUserManager();
            return userManager.loadUser(uuid, null).thenCompose(user ->
                    applyRankGroup(user, targetGroup) ? userManager.saveUser(user) : CompletableFuture.completedFuture(null));
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Konnte LuckPerms-Rang für " + uuid + " nicht auf '" + targetGroup + "' synchronisieren", ex);
            return null;
        });
    }

    @Override
    public void grantOwner(UUID uuid) {
        ensureGroup(OWNER_GROUP, true).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Konnte Owner-Gruppe nicht sicherstellen", ex);
            return null;
        });
        UserManager userManager = luckPerms.getUserManager();
        User loaded = userManager.getUser(uuid);
        if (loaded != null) {
            saveOwnerChanges(userManager, loaded, uuid);
            return;
        }
        userManager.loadUser(uuid, null).thenAccept(user -> saveOwnerChanges(userManager, user, uuid)).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Konnte Owner-Rechte für " + uuid + " nicht setzen", ex);
            return null;
        });
    }

    @Override
    public void grantPermission(UUID uuid, String permission) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(permission, "permission");
        UserManager userManager = luckPerms.getUserManager();
        User loaded = userManager.getUser(uuid);
        if (loaded != null) {
            savePermissionChange(userManager, loaded, uuid, permission);
            return;
        }
        userManager.loadUser(uuid, null).thenAccept(user -> savePermissionChange(userManager, user, uuid, permission)).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Konnte Permission '" + permission + "' für " + uuid + " nicht setzen", ex);
            return null;
        });
    }

    private void savePermissionChange(UserManager userManager, User user, UUID uuid, String permission) {
        try {
            Node node = Node.builder(permission).value(true).build();
            if (user.data().add(node) == DataMutateResult.SUCCESS) {
                userManager.saveUser(user).exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Konnte Permission '" + permission + "' für " + uuid + " nicht speichern", ex);
                    return null;
                });
            }
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Konnte Permission '" + permission + "' für " + uuid + " nicht anwenden", ex);
        }
    }

    private void saveOwnerChanges(UserManager userManager, User user, UUID uuid) {
        try {
            if (applyOwner(user)) {
                userManager.saveUser(user).exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Konnte Owner-User " + uuid + " nicht speichern", ex);
                    return null;
                });
            }
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Konnte Owner-Rechte für " + uuid + " nicht anwenden", ex);
        }
    }

    private boolean applyOwner(User user) {
        boolean changed = false;
        NodeMap data = user.data();
        if (data.add(InheritanceNode.builder(OWNER_GROUP).build()) == DataMutateResult.SUCCESS) changed = true;
        if (!OWNER_GROUP.equalsIgnoreCase(user.getPrimaryGroup())
                && user.setPrimaryGroup(OWNER_GROUP) == DataMutateResult.SUCCESS) changed = true;
        if (data.add(Node.builder("*").value(true).build()) == DataMutateResult.SUCCESS) changed = true;
        return changed;
    }

    private void ensureServerGroups() {
        for (String groupName : RankGroupMapping.managedGroupNames()) ensureGroup(groupName, false);
        for (String groupName : TEAM_GROUPS) {
            final boolean owner = OWNER_GROUP.equals(groupName);
            ensureGroup(groupName, owner).thenCompose(ignored -> applyStaffPermissions(groupName)).exceptionally(ex -> {
                logger.log(Level.WARNING, "Teamgruppe konnte nicht vollständig vorbereitet werden: " + groupName, ex);
                return null;
            });
        }
    }

    private CompletableFuture<Void> applyStaffPermissions(String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) return CompletableFuture.completedFuture(null);
        boolean changed = false;
        if (TEAM_GROUPS.contains(groupName)) {
            for (String permission : TEAM_PERMISSIONS) {
                if (group.data().add(Node.builder(permission).value(true).build()) == DataMutateResult.SUCCESS) changed = true;
            }
        }
        if (ADMIN_PLUS_GROUPS.contains(groupName)) {
            for (String permission : ADMIN_PERMISSIONS) {
                if (group.data().add(Node.builder(permission).value(true).build()) == DataMutateResult.SUCCESS) changed = true;
            }
        }
        return changed ? luckPerms.getGroupManager().saveGroup(group) : CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> ensureGroup(String groupName, boolean owner) {
        Group existing = luckPerms.getGroupManager().getGroup(groupName);
        if (existing != null) {
            if (owner) {
                existing.data().add(Node.builder("*").value(true).build());
                return luckPerms.getGroupManager().saveGroup(existing);
            }
            return CompletableFuture.completedFuture(null);
        }
        return luckPerms.getGroupManager().createAndLoadGroup(groupName).thenCompose(group -> {
            if (owner) group.data().add(Node.builder("*").value(true).build());
            logger.info("LuckPerms-Gruppe automatisch angelegt: " + groupName);
            return luckPerms.getGroupManager().saveGroup(group);
        });
    }

    private boolean applyRankGroup(User user, String targetGroup) {
        NodeMap data = user.data();
        Collection<String> managedGroups = RankGroupMapping.managedGroupNames();
        List<Node> inheritanceNodes = data.toCollection().stream()
                .filter(NodeType.INHERITANCE::matches)
                .collect(Collectors.toList());

        boolean changed = false;
        boolean hasTarget = false;
        for (Node node : inheritanceNodes) {
            String groupName = NodeType.INHERITANCE.cast(node).getGroupName();
            if (groupName.equalsIgnoreCase(targetGroup)) {
                hasTarget = true;
                continue;
            }
            boolean managed = managedGroups.stream().anyMatch(name -> name.equalsIgnoreCase(groupName));
            if (managed) {
                data.remove(node);
                changed = true;
            }
        }
        if (!hasTarget) {
            data.add(InheritanceNode.builder(targetGroup).build());
            changed = true;
        }

        String currentPrimary = user.getPrimaryGroup() == null ? "" : user.getPrimaryGroup().toLowerCase();
        if (!TEAM_GROUPS.contains(currentPrimary)
                && user.setPrimaryGroup(targetGroup) == DataMutateResult.SUCCESS) changed = true;
        return changed;
    }
}
