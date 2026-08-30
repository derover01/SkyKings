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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** LuckPerms-Anbindung fuer SkyKings. Gameplay-Rang bleibt in SkyKings die Source of Truth. */
public final class LuckPermsPermissionBridge implements PermissionBridge {

    private static final String OWNER_GROUP = "owner";

    public static PermissionBridge createIfAvailable(Logger logger) {
        RegisteredServiceProvider<LuckPerms> registration =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) {
            return new NoOpPermissionBridge();
        }
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

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void syncRank(UUID uuid, Rank rank) {
        String targetGroup = RankGroupMapping.groupNameFor(rank);
        if (targetGroup == null) {
            logger.warning("Kein LuckPerms-Gruppen-Mapping fuer Rank " + rank + " definiert.");
            return;
        }

        ensureGroup(targetGroup, false).thenCompose(ignored -> {
            UserManager userManager = luckPerms.getUserManager();
            return userManager.loadUser(uuid, null).thenCompose(user ->
                    applyRankGroup(user, targetGroup)
                            ? userManager.saveUser(user)
                            : CompletableFuture.completedFuture(null));
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Konnte LuckPerms-Rang fuer " + uuid + " nicht auf '"
                    + targetGroup + "' synchronisieren", ex);
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

        userManager.loadUser(uuid, null)
                .thenAccept(user -> saveOwnerChanges(userManager, user, uuid))
                .exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Konnte Owner-Rechte fuer " + uuid + " nicht setzen", ex);
                    return null;
                });
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
            logger.log(Level.SEVERE, "Konnte Owner-Rechte fuer " + uuid + " nicht anwenden", ex);
        }
    }

    private boolean applyOwner(User user) {
        boolean changed = false;
        NodeMap data = user.data();

        InheritanceNode ownerNode = InheritanceNode.builder(OWNER_GROUP).build();
        if (data.add(ownerNode) == DataMutateResult.SUCCESS) {
            changed = true;
        }

        // Primaere Gruppe zuerst setzen. So bleibt der Owner-Status selbst dann korrekt,
        // wenn eine optionale Permission-Node spaeter unerwartet fehlschlaegt.
        if (!OWNER_GROUP.equalsIgnoreCase(user.getPrimaryGroup())
                && user.setPrimaryGroup(OWNER_GROUP) == DataMutateResult.SUCCESS) {
            changed = true;
        }

        // Allgemeine Node-API ist auf LuckPerms 5.4/Java 8 robuster als der spezialisierte Builder.
        Node wildcard = Node.builder("*").value(true).build();
        if (data.add(wildcard) == DataMutateResult.SUCCESS) {
            changed = true;
        }
        return changed;
    }

    /** Erstellt alle SkyKings-Ranggruppen plus Owner asynchron, falls sie noch fehlen. */
    private void ensureServerGroups() {
        for (String groupName : RankGroupMapping.managedGroupNames()) {
            ensureGroup(groupName, false);
        }
        ensureGroup(OWNER_GROUP, true);
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
            if (owner) {
                group.data().add(Node.builder("*").value(true).build());
            }
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
        boolean isOwner = false;

        for (Node node : inheritanceNodes) {
            String groupName = NodeType.INHERITANCE.cast(node).getGroupName();
            if (OWNER_GROUP.equalsIgnoreCase(groupName)) {
                isOwner = true;
                continue;
            }
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

        if (!isOwner && user.setPrimaryGroup(targetGroup) == DataMutateResult.SUCCESS) {
            changed = true;
        }

        return changed;
    }
}
