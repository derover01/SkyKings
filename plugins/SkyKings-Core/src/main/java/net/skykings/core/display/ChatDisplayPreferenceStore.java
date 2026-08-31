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

    /** Standard AN, damit bestehendes Verhalten fuer alle Spieler erhalten bleibt. */
    public boolean showRankWithCosmeticPrefix(UUID uuid) {
        return yaml.getBoolean(path(uuid), true);
    }

    public void setShowRankWithCosmeticPrefix(UUID uuid, boolean show) {
        yaml.set(path(uuid), show);
        save();
    }

    private String path(UUID uuid) {
        return "players." + uuid.toString() + ".show-rank-with-prefix";
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Chat-Anzeige konnte nicht gespeichert werden.", ex);
        }
    }
}
