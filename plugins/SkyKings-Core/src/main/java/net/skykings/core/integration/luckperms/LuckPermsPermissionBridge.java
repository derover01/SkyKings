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

import java.util.ArrayList;
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
 * Gruppen in LuckPerms angelegt; die 11 Rang-Gruppen muessen dort bereits existieren -
 * {@link #syncRank(UUID, Rank)} prueft das vor jeder Synchronisierung und tut bei einer
 * fehlenden Zielgruppe rein gar nichts (kein Node wird veraendert, keine Gruppe wird angelegt).
 */
public final class LuckPermsPermissionBridge implements PermissionBridge {

    /** Liefert eine echte Bridge falls LuckPerms als Service registriert ist, sonst eine No-Op-Bridge. */
    public static PermissionBridge createIfAvailable(Logger logger) {
        RegisteredServiceProvider<LuckPerms> registration =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) {
            return new NoOpPermissionBridge();
        }
        LuckPermsPermissionBridge bridge = new LuckPermsPermissionBridge(registration.getProvider(), logger);
        bridge.warnAboutMissingGroups();
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
            logger.warning("Kein LuckPerms-Gruppen-Mapping fuer Rank " + rank + " definiert - Synchronisation uebersprungen.");
            return;
        }

        if (!groupExists(targetGroup)) {
            logger.warning("SkyKings-Rang konnte nicht synchronisiert werden, weil die LuckPerms-Gruppe fehlt. "
                    + "Gruppe='" + targetGroup + "', Spieler=" + uuid + ", Rang=" + rank + ". SkyKings legt "
                    + "Ranggruppen nicht automatisch an - bitte die Gruppe in LuckPerms anlegen.");
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

    private boolean groupExists(String groupName) {
        return luckPerms.getGroupManager().getGroup(groupName) != null;
    }

    /**
     * Optionale Startpruefung (siehe Auftrag Phase-1B-Hardening): sammelt alle von SkyKings
     * benoetigten Gruppen, die in LuckPerms fehlen, und loggt sie einmalig gesammelt als
     * WARNING. Legt keine Gruppen an.
     */
    private void warnAboutMissingGroups() {
        List<String> missing = new ArrayList<>();
        for (String groupName : RankGroupMapping.managedGroupNames()) {
            if (!groupExists(groupName)) {
                missing.add(groupName);
            }
        }
        if (!missing.isEmpty()) {
            logger.warning("Folgende SkyKings-Ranggruppen fehlen in LuckPerms und muessen manuell angelegt "
                    + "werden (SkyKings legt sie NICHT automatisch an): " + String.join(", ", missing));
        }
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
