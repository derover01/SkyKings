package net.skykings.core;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * SkyKings-Core - Phase 0 Grundgeruest.
 *
 * <p>Verantwortungsbereich laut docs/ARCHITECTURE.md: Player-Profile, Rangmodell/interne
 * Rank-API, Economy-API, Kit-Registry und Cooldown-Basis, zentrale Configs, gemeinsame
 * GUI-/Item-Utilities, Datenpersistenz sowie Events/API fuer die anderen SkyKings-Module.
 *
 * <p>In Phase 0 (siehe docs/ROADMAP.md) wird hier bewusst noch KEINE Business-Logik
 * implementiert - nur ein kompilierbares Plugin-Grundgeruest.
 */
public class SkyKingsCore extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("SkyKings-Core (Phase 0 Grundgeruest) aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Core deaktiviert.");
    }
}
