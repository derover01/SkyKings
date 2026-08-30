package net.skykings.admin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * SkyKings-Admin - Phase 0 Grundgeruest.
 *
 * <p>Verantwortungsbereich laut docs/ARCHITECTURE.md: /gutscheine GUI, /verlosung,
 * Admin-/Economy-Audit, Staff Utility Commands, Discord Logging Bridge, sensible
 * Aktionen und Berechtigungspruefungen.
 *
 * <p>In Phase 0 (siehe docs/ROADMAP.md) wird hier bewusst noch KEINE Business-Logik
 * implementiert - nur ein kompilierbares Plugin-Grundgeruest.
 */
public class SkyKingsAdmin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("SkyKings-Admin (Phase 0 Grundgeruest) aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Admin deaktiviert.");
    }
}
