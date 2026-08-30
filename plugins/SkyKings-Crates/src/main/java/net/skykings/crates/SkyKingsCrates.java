package net.skykings.crates;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * SkyKings-Crates - Phase 0 Grundgeruest.
 *
 * <p>Verantwortungsbereich laut docs/ARCHITECTURE.md: Crates als Custom-Heads im
 * Inventar, Linksklick Preview / Rechtsklick Oeffnen, Open-All ab Exile, /craterewards,
 * rankabhaengige Claim-Cooldowns, Rank-/Kit-/Permission-/Prefix-Gutscheine, Unique
 * Voucher IDs, Anti-Dupe, Reward- und Crate-Logs.
 *
 * <p>In Phase 0 (siehe docs/ROADMAP.md) wird hier bewusst noch KEINE Business-Logik
 * implementiert - nur ein kompilierbares Plugin-Grundgeruest. Fachlich gehoeren Crates
 * laut Roadmap zu Phase 4.
 */
public class SkyKingsCrates extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("SkyKings-Crates (Phase 0 Grundgeruest) aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Crates deaktiviert.");
    }
}
