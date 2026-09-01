package net.skykings.core.display;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/** Persistiert reine Anzeige-Praeferenzen getrennt von Rang- und Permissiondaten. */
public final class ChatDisplayPreferenceStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public ChatDisplayPreferenceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chat-display.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Rang ist jetzt ein echter eigenstaendiger Chat-Layer. Fuer bestehende Spieler wird der
     * alte show-rank-with-prefix-Wert als Migration-Fallback uebernommen.
     */
    public boolean showRank(UUID uuid) {
        String newPath = path(uuid, "show-rank");
        if (yaml.contains(newPath)) return yaml.getBoolean(newPath, true);
        return yaml.getBoolean(path(uuid, "show-rank-with-prefix"), true);
    }

    public void setShowRank(UUID uuid, boolean show) {
        yaml.set(path(uuid, "show-rank"), show);
        // Alten Key synchron halten, damit Downgrade/Teststaende nicht widerspruechlich werden.
        yaml.set(path(uuid, "show-rank-with-prefix"), show);
        save();
    }

    /** Backward-compatible API fuer bestehenden Code. */
    public boolean showRankWithCosmeticPrefix(UUID uuid) {
        return showRank(uuid);
    }

    /** Backward-compatible API fuer bestehenden Code. */
    public void setShowRankWithCosmeticPrefix(UUID uuid, boolean show) {
        setShowRank(uuid, show);
    }

    /** Der Besitz bleibt erhalten; nur die sichtbare Chat-Ausgabe kann ausgeblendet werden. */
    public boolean showCosmeticPrefix(UUID uuid) {
        return yaml.getBoolean(path(uuid, "show-cosmetic-prefix"), true);
    }

    public void setShowCosmeticPrefix(UUID uuid, boolean show) {
        yaml.set(path(uuid, "show-cosmetic-prefix"), show);
        save();
    }

    /** Clan-Mitgliedschaft bleibt erhalten; nur der Tag im normalen Chat wird ausgeblendet. */
    public boolean showClanTag(UUID uuid) {
        return yaml.getBoolean(path(uuid, "show-clan-tag"), true);
    }

    public void setShowClanTag(UUID uuid, boolean show) {
        yaml.set(path(uuid, "show-clan-tag"), show);
        save();
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid.toString() + "." + key;
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Chat-Anzeige konnte nicht gespeichert werden.", ex);
        }
    }
}
