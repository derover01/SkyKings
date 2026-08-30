package net.skykings.core.display;

import net.skykings.core.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RankDisplayConfig {

    private final YamlConfiguration config;
    private final Set<String> ownerNames = new HashSet<>();

    public RankDisplayConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "rank-display.yml");
        if (!file.exists()) {
            plugin.saveResource("rank-display.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        List<String> owners = config.getStringList("owner-names");
        for (String owner : owners) {
            if (owner != null && !owner.trim().isEmpty()) {
                ownerNames.add(owner.toLowerCase(Locale.ROOT));
            }
        }
    }

    public boolean isConfiguredOwner(String playerName) {
        return playerName != null && ownerNames.contains(playerName.toLowerCase(Locale.ROOT));
    }

    public String getOwnerPrefix() {
        return color(config.getString("owner-prefix", "&4&lOWNER"));
    }

    public String getRankPrefix(Rank rank) {
        String raw = config.getString("ranks." + rank.name(), "&7" + rank.name());
        return color(raw);
    }

    private static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }
}
