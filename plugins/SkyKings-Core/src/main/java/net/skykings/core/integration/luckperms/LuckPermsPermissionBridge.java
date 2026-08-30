package net.skykings.core.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
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

/**
 * Echte LuckPerms-Bridge (Phase 1B). Referenziert LuckPerms-API-Typen nur in dieser Klasse,
 * damit ein fehlendes LuckPerms nie zu einer harten {@code ClassNotFoundException}/
 * {@code NoClassDefFoundError} beim Laden von {@code SkyKingsCore} fuehrt - diese Klasse wird
 * erst geladen (und damit erst verlinkt), wenn {@link #createIfAvailable(Logger)} aufgerufen
 * wird, und das geschieht in {@code SkyKingsCore} innerhalb eines try/catch(Throwable).
 *
 * <p>Richtung ist bewusst einseitig SkyKings -&gt; LuckPerms (siehe {@link PermissionBridge}).
 * Es werden ausschliesslich die von SkyKings verwalteten Rang-Gruppen
 * ({@link RankGroupMapping#managedGroupNames()}) angefasst - andere Gruppen (z. B.
 * Team-/Custom-Gruppen eines Server-Betreibers) bleiben unangetastet. Es werden auch keine
 * Gruppen in LuckPerms angelegt; die 11 Rang-Gruppen muessen dort bereits existieren.
 */
public final class LuckPermsPermissionBridge implements PermissionBridge {

    /** Liefert eine echte Bridge falls LuckPerms als Service registriert ist, sonst eine No-Op-Bridge. */
    public static PermissionBridge createIfAvailable(Logger logger) {
        RegisteredServiceProvider<LuckPerms> registration =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) {
            return new NoOpPermissionBridge();
        }
        return new LuckPermsPermissionBridge(registration.getProvider(), logger);
    }

    private final LuckPerms luckPerms;
    private final Logger logger;

    private LuckPermsPermissionBridge(LuckPerms luckPerms, Logger logger) {
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
            logger.warning("Kein LuckPerms-Gruppen-Mapping fuer Rank " + rank + " definiert - Synchronisation uebersprungen.");
            return;
        }

        UserManager userManager = luckPerms.getUserManager();
        userManager.loadUser(uuid)
                .thenCompose(user -> applyRankGroup(user, targetGroup)
                        ? userManager.saveUser(user)
                        : CompletableFuture.completedFuture(null))
                .exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Konnte LuckPerms-Gruppe fuer " + uuid + " nicht auf '"
                            + targetGroup + "' synchronisieren", ex);
                    return null;
                });
    }

    /**
     * Entfernt alle anderen von SkyKings verwalteten Rang-Gruppen und stellt sicher, dass die
     * Zielgruppe als Inheritance-Node + primaere Gruppe gesetzt ist. Liefert {@code true}, falls
     * tatsaechlich etwas geaendert wurde (nur dann muss gespeichert werden).
     */
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
            boolean isManagedBySkyKings = managedGroups.stream().anyMatch(managed -> managed.equalsIgnoreCase(groupName));
            if (isManagedBySkyKings) {
                data.remove(node);
                changed = true;
            }
        }

        if (!hasTarget) {
            data.add(InheritanceNode.builder(targetGroup).build());
            changed = true;
        }

        if (user.setPrimaryGroup(targetGroup) == DataMutateResult.SUCCESS) {
            changed = true;
        }

        return changed;
    }
}
