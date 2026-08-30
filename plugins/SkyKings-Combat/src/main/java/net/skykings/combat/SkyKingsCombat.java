package net.skykings.combat;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * SkyKings-Combat - Phase 0 Grundgeruest.
 *
 * <p>Verantwortungsbereich laut docs/ARCHITECTURE.md: kein Fallschaden, Starter-Kit nach
 * jedem Tod, Combat Tag, 3-Sekunden-Enderpearl-Cooldown, Kill-/Death-Verarbeitung,
 * Nethersterne pro Kill und Killstreak-Multiplikatoren, Anti-Killfarm, Lootschutz nach
 * Kill, Newbie Protection, Hot Zones, King Zone, Loot-Chest-Events, Booster-Druckplatten.
 *
 * <p>In Phase 0 (siehe docs/ROADMAP.md) wird hier bewusst noch KEINE Business-Logik
 * implementiert - nur ein kompilierbares Plugin-Grundgeruest. Fachlich gehoert Combat
 * laut Roadmap zu Phase 2.
 */
public class SkyKingsCombat extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("SkyKings-Combat (Phase 0 Grundgeruest) aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Combat deaktiviert.");
    }
}
